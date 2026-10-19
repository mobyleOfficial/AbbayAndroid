package com.mobyle.abbay.presentation.cast

import com.mobyle.abbay.presentation.playback.remote.ProcessedAudioStream
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class LocalMediaServerTest {
    private val file = ByteArray(100_000) { (it % 251).toByte() }
    private val server = LocalMediaServer(InetAddress.getLoopbackAddress(), opener = { index, offset ->
        if (index != 0) {
            null
        } else {
            LocalMediaServer.OpenedFile(
                file.size.toLong(),
                "audio/mpeg",
                ByteArrayInputStream(
                    file,
                    offset.toInt(),
                    file.size - offset.toInt()
                )
            )
        }
    }).apply { start() }

    @After
    fun stop() = server.stop()

    @Test
    fun servesTheWholeFileAndRanges() {
        val whole = connect(server.urlOf(0))
        assertEquals(200, whole.responseCode)
        assertEquals("audio/mpeg", whole.contentType)
        assertArrayEquals(file, whole.inputStream.readBytes())

        val part = connect(server.urlOf(0)) { setRequestProperty("Range", "bytes=1000-1999") }
        assertEquals(206, part.responseCode)
        assertEquals("bytes 1000-1999/100000", part.getHeaderField("Content-Range"))
        assertArrayEquals(file.copyOfRange(1_000, 2_000), part.inputStream.readBytes())

        val tail = connect(server.urlOf(0)) { setRequestProperty("Range", "bytes=99990-") }
        assertArrayEquals(file.copyOfRange(99_990, 100_000), tail.inputStream.readBytes())
    }

    @Test
    fun anythingButTheSessionsFilesIsRefused() {
        assertEquals(404, connect(server.urlOf(0).replace(server.token, "0".repeat(32))).responseCode)
        assertEquals(404, connect(server.urlOf(1)).responseCode)
        assertEquals(404, connect("http://127.0.0.1:${server.port}/").responseCode)
        assertEquals(
            405,
            connect(server.urlOf(0)) {
                requestMethod = "POST"
                doOutput = true
            }.responseCode
        )
        assertEquals(416, connect(server.urlOf(0)) { setRequestProperty("Range", "bytes=200000-") }.responseCode)
        assertTrue(server.token.length == 32)
    }

    @Test
    fun rangeParsing() {
        assertEquals(0L to 99L, LocalMediaServer.parseRange("bytes=0-99", 1_000))
        assertEquals(900L to 999L, LocalMediaServer.parseRange("bytes=-100", 1_000))
        assertEquals(500L to 999L, LocalMediaServer.parseRange("bytes=500-5000", 1_000))
        assertNull(LocalMediaServer.parseRange("bytes=0-1,5-6", 1_000))
        assertNull(LocalMediaServer.parseRange("items=0-1", 1_000))
    }

    private fun connect(url: String, configure: HttpURLConnection.() -> Unit = {}): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            configure()
            if (doOutput) outputStream.close()
        }

    @Test
    fun servesTheLiveStreamOfTheCurrentGenerationUntilANewOneBegins() {
        val stream = ProcessedAudioStream()
        val first = stream.beginGeneration(sampleRateHz = 48_000, channelCount = 2)
        val liveServer = LocalMediaServer(InetAddress.getLoopbackAddress(), { _, _ -> null }, stream.asLiveSource()).apply { start() }
        try {
            val pcm = ByteArray(4_000) { (it % 127).toByte() }
            stream.write(java.nio.ByteBuffer.wrap(pcm))

            val connection = connect(liveServer.liveUrlOf(first))
            assertEquals(200, connection.responseCode)
            assertEquals("audio/wav", connection.contentType)
            val body = connection.inputStream
            val header = ByteArray(44).also { body.readNBytes(it, 0, it.size) }
            assertArrayEquals(ProcessedAudioStream.wavHeader(48_000, 2), header)
            assertArrayEquals(pcm, body.readNBytes(pcm.size))

            val second = stream.beginGeneration(sampleRateHz = 48_000, channelCount = 2)
            assertEquals(-1, body.read())
            assertEquals(404, connect(liveServer.liveUrlOf(first)).responseCode)
            assertEquals(200, connect(liveServer.liveUrlOf(second)) { requestMethod = "HEAD" }.responseCode)
        } finally {
            stream.close()
            liveServer.stop()
        }
    }
}
