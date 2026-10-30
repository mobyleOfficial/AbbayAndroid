package com.mobyle.abbay.data.librivox

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

class HttpStatusException(val status: Int) : IOException("HTTP $status")

@Singleton
class LibriVoxHttp @Inject constructor() {
    fun getText(url: String, query: Map<String, String> = emptyMap(), timeoutMs: Int = DEFAULT_TIMEOUT_MS): String {
        val connection = connect(withQuery(url, query), timeoutMs)
        try {
            checkedStatus(connection)
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun getBytes(url: String, maxBytes: Int): ByteArray {
        val connection = connect(url, DEFAULT_TIMEOUT_MS)
        try {
            checkedStatus(connection)
            val bytes = connection.inputStream.use { it.readAtMost(maxBytes + 1) }
            if (bytes.size > maxBytes) throw IOException("Response too large")
            return bytes
        } finally {
            connection.disconnect()
        }
    }

    fun download(url: String, destination: File, isStopped: () -> Boolean, onProgress: (received: Long, total: Long) -> Unit) {
        val partial = File(destination.parentFile, destination.name + PARTIAL_SUFFIX)
        val alreadyReceived = if (partial.exists()) partial.length() else 0L
        val headers = if (alreadyReceived > 0) mapOf("Range" to "bytes=$alreadyReceived-") else emptyMap()
        val connection = connect(url, DOWNLOAD_TIMEOUT_MS, headers)
        try {
            val isResumed = checkedStatus(connection) == HttpURLConnection.HTTP_PARTIAL
            val offset = if (isResumed) alreadyReceived else 0L
            val total = connection.contentLengthLong.takeIf { it > 0 }?.plus(offset) ?: -1L
            val received = connection.inputStream.use { input ->
                FileOutputStream(partial, isResumed).use { output ->
                    copy(input, output, offset, isStopped) { received -> onProgress(received, total) }
                }
            }
            if (total > 0 && received < total) throw IOException("Incomplete download")
            if (!partial.renameTo(destination)) throw IOException("Could not move ${partial.name}")
        } finally {
            connection.disconnect()
        }
    }

    private fun copy(
        input: InputStream,
        output: OutputStream,
        offset: Long,
        isStopped: () -> Boolean,
        onReceived: (received: Long) -> Unit,
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var received = offset
        var count = input.read(buffer)
        while (count >= 0) {
            if (isStopped()) throw IOException("Stopped")
            output.write(buffer, 0, count)
            received += count
            onReceived(received)
            count = input.read(buffer)
        }
        return received
    }

    private fun checkedStatus(connection: HttpURLConnection): Int {
        val status = connection.responseCode
        if (status !in SUCCESS) throw HttpStatusException(status)
        return status
    }

    private fun connect(url: String, timeoutMs: Int, headers: Map<String, String> = emptyMap()): HttpURLConnection {
        var target = URL(url)
        repeat(MAX_REDIRECTS) {
            if (!isAllowed(target)) throw IOException("Host not allowed: ${target.host}")
            val connection = (target.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json, */*")
                headers.forEach { (name, value) -> setRequestProperty(name, value) }
            }
            val location = connection.getHeaderField("Location")
            if (connection.responseCode !in REDIRECTS || location == null) return connection
            connection.disconnect()
            target = URL(target, location)
        }
        throw IOException("Too many redirects")
    }

    private fun withQuery(url: String, query: Map<String, String>): String {
        if (query.isEmpty()) return url
        return url + "?" + query.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    }

    private fun encode(text: String) = URLEncoder.encode(text, Charsets.UTF_8.name())

    private fun InputStream.readAtMost(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var count = read(buffer, 0, minOf(buffer.size, limit))
        while (count >= 0 && output.size() < limit) {
            output.write(buffer, 0, count)
            count = read(buffer, 0, minOf(buffer.size, limit - output.size()).coerceAtLeast(1))
        }
        return output.toByteArray()
    }

    companion object {
        const val PARTIAL_SUFFIX = ".part"
        private const val DEFAULT_TIMEOUT_MS = 20_000
        private const val DOWNLOAD_TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_REDIRECTS = 5
        private val SUCCESS = 200..299
        private val REDIRECTS = 300..399
        private val ALLOWED_HOSTS = listOf("archive.org", "librivox.org")

        fun isAllowed(url: URL): Boolean {
            val host = url.host.lowercase()
            return url.protocol == "https" && ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }
        }
    }
}
