package com.mobyle.abbay.presentation.cast

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("TooManyFunctions")
class LocalMediaServer(
    private val bindAddress: InetAddress,
    private val opener: FileOpener,
    private val live: LiveSource? = null,
) {
    fun interface FileOpener {
        fun open(index: Int, offset: Long): OpenedFile?
    }

    class OpenedFile(val length: Long, val mimeType: String, val stream: InputStream)

    interface LiveSource {
        val mimeType: String

        fun header(generation: Int): ByteArray?

        fun open(generation: Int): InputStream?
    }

    private var socket: ServerSocket? = null
    private var executor: ExecutorService? = null

    val token: String = buildToken()

    val port: Int get() = socket?.localPort ?: -1

    fun urlOf(index: Int): String = "http://${bindAddress.hostAddress}:$port/$token/$index"

    fun liveUrlOf(generation: Int): String = "http://${bindAddress.hostAddress}:$port/$token/$LIVE/$generation"

    fun start() {
        if (socket != null) return
        val server = ServerSocket(0, BACKLOG, bindAddress)
        socket = server
        val pool = Executors.newFixedThreadPool(WORKERS).also { executor = it }
        pool.execute {
            while (!server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                pool.execute { client.use(::serve) }
            }
        }
    }

    fun stop() {
        runCatching { socket?.close() }
        socket = null
        executor?.shutdownNow()
        executor = null
    }

    private fun serve(client: Socket) {
        client.soTimeout = READ_TIMEOUT_MS
        val input = client.getInputStream()
        val output = client.getOutputStream()
        val requestLine = readLine(input) ?: return
        val headers = generateSequence { readLine(input)?.takeIf(String::isNotEmpty) }
            .mapNotNull { line ->
                line.indexOf(':').takeIf { it > 0 }?.let {
                    line.substring(0, it).trim().lowercase() to
                        line.substring(it + 1).trim()
                }
            }
            .toMap()
        val parts = requestLine.split(' ')
        val method = parts.getOrNull(0)
        val segments = parts.getOrNull(1)?.trim('/')?.split('/').orEmpty()
        val index = segments.getOrNull(1)?.toIntOrNull()
        val liveGeneration = segments.takeIf { it.size == LIVE_SEGMENTS && it[1] == LIVE }?.get(2)?.toIntOrNull()
        when {
            method != "GET" && method != "HEAD" -> respond(output, STATUS_METHOD_NOT_ALLOWED)
            segments.getOrNull(0) != token -> respond(output, STATUS_NOT_FOUND)
            liveGeneration != null -> sendLive(output, liveGeneration, isHead = method == "HEAD")
            segments.size != 2 || index == null -> respond(output, STATUS_NOT_FOUND)
            else -> sendFile(output, index, headers["range"], isHead = method == "HEAD")
        }
    }

    @Suppress("ReturnCount")
    private fun sendFile(output: OutputStream, index: Int, range: String?, isHead: Boolean) {
        val probe = opener.open(index, 0) ?: return respond(output, STATUS_NOT_FOUND)
        val length = probe.length
        probe.stream.close()
        val requested = range?.let { parseRange(it, length) }
        if (range != null && requested == null) return respond(output, STATUS_RANGE_NOT_SATISFIABLE, "Content-Range: bytes */$length\r\n")
        val (start, end) = requested ?: (0L to length - 1)
        val headers = buildString {
            append("Content-Type: ${probe.mimeType}\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Content-Length: ${end - start + 1}\r\n")
            if (requested != null) append("Content-Range: bytes $start-$end/$length\r\n")
        }
        respond(output, if (requested != null) STATUS_PARTIAL else STATUS_OK, headers, flushOnly = true)
        if (isHead) return
        val file = opener.open(index, start) ?: return
        file.stream.use { stream -> copy(stream, output, end - start + 1) }
        output.flush()
    }

    private fun sendLive(output: OutputStream, generation: Int, isHead: Boolean) {
        val source = live ?: return respond(output, STATUS_NOT_FOUND)
        val header = source.header(generation) ?: return respond(output, STATUS_NOT_FOUND)
        val headers = "Content-Type: ${source.mimeType}\r\nCache-Control: no-cache\r\nAccept-Ranges: none\r\n"
        respond(output, STATUS_OK, headers, flushOnly = true)
        if (isHead) return
        val audio = source.open(generation) ?: return
        try {
            output.write(header)
            audio.use { copy(it, output, Long.MAX_VALUE) }
            output.flush()
        } catch (_: SocketException) {
        }
    }

    private fun copy(stream: InputStream, output: OutputStream, count: Long) {
        val buffer = ByteArray(BUFFER_SIZE)
        var remaining = count
        try {
            while (remaining > 0) {
                val read = stream.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        } catch (_: SocketException) {
        }
    }

    private fun respond(output: OutputStream, status: String, headers: String = "", flushOnly: Boolean = false) {
        val text = "HTTP/1.1 $status\r\n$headers" + (if (flushOnly) "" else "Content-Length: 0\r\n") + "Connection: close\r\n\r\n"
        output.write(text.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    @Suppress("ReturnCount")
    private fun readLine(input: InputStream): String? {
        val line = StringBuilder()
        while (line.length < MAXIMUM_LINE) {
            val byte = input.read()
            if (byte < 0) return line.toString().takeIf { it.isNotEmpty() }
            if (byte == '\n'.code) return line.toString().trimEnd('\r')
            line.append(byte.toChar())
        }
        return null
    }

    private fun buildToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(SecureRandom()::nextBytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun parseRange(header: String, length: Long): Pair<Long, Long>? {
            val value = header.removePrefix("bytes=").takeIf { header.startsWith("bytes=") && !it.contains(',') } ?: return null
            val (first, last) = value.split('-', limit = 2).let { (it.getOrNull(0).orEmpty()) to (it.getOrNull(1).orEmpty()) }
            val range = when {
                first.isEmpty() -> last.toLongOrNull()?.let { suffix -> (length - suffix).coerceAtLeast(0L) to length - 1 }
                else -> first.toLongOrNull()?.let { start -> start to (last.toLongOrNull() ?: (length - 1)).coerceAtMost(length - 1) }
            }
            return range?.takeIf { (start, end) -> start in 0..end && end < length }
        }

        private const val LIVE = "live"

        private const val LIVE_SEGMENTS = 3
        private const val BACKLOG = 8
        private const val WORKERS = 4
        private const val READ_TIMEOUT_MS = 15_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAXIMUM_LINE = 8 * 1024
        private const val TOKEN_BYTES = 16
        private const val STATUS_OK = "200 OK"
        private const val STATUS_PARTIAL = "206 Partial Content"
        private const val STATUS_NOT_FOUND = "404 Not Found"
        private const val STATUS_METHOD_NOT_ALLOWED = "405 Method Not Allowed"
        private const val STATUS_RANGE_NOT_SATISFIABLE = "416 Range Not Satisfiable"
    }
}
