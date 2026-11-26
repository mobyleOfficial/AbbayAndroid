package com.mobyle.abbay.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream

class Mp3DurationTest {
    private fun fixture(name: String) = File(requireNotNull(javaClass.classLoader?.getResource(name)).toURI())

    @Test
    fun `a file without a duration header needs a full scan and gets its exact duration`() {
        val file = fixture("vbr_no_xing.mp3")

        assertTrue(Mp3Duration.needsFullScan(file.inputStream()))
        assertEquals(60_000.0, file.inputStream().use { Mp3Duration.scan(it.buffered(), file.length()) }!!.toDouble(), 100.0)
    }

    @Test
    fun `a file with a Xing header keeps the header duration`() {
        assertFalse(Mp3Duration.needsFullScan(fixture("vbr_with_xing.mp3").inputStream()))
    }

    @Test
    fun `the ID3 tag is skipped before looking for the header`() {
        val id3Body = ByteArray(300) { 'X'.code.toByte() }
        val id3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 2, 44)
        val frame = ByteArray(100) + "Info".toByteArray() + ByteArray(100)

        assertFalse(Mp3Duration.needsFullScan((id3Header + id3Body + frame).inputStream()))
        assertTrue(Mp3Duration.needsFullScan((id3Header + "Xing".toByteArray() + ByteArray(296) + ByteArray(200)).inputStream()))
    }

    @Test
    fun `a corrupt ID3 size larger than the file needs a full scan without buffering the tag`() {
        val hugeId3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0x7F, 0x7F, 0x7F, 0x7F)

        assertTrue(Mp3Duration.needsFullScan((hugeId3Header + ByteArray(500)).inputStream()))
    }

    @Test
    fun `a full scan skips a huge ID3 tag instead of buffering it`() {
        val audio = fixture("vbr_no_xing.mp3").readBytes()
        val tagBodySize = 0x0FFF_FFFF
        val stream = PaddedId3Stream(tagBodySize, audio)

        val duration = Mp3Duration.scan(stream, ID3_HEADER_BYTES + tagBodySize.toLong() + audio.size)

        assertEquals(60_000.0, duration!!.toDouble(), 100.0)
        assertTrue("read ${stream.tagBytesRead} tag bytes", stream.tagBytesRead < 64 * 1024)
    }

    @Test
    fun `a full scan of a tag larger than the file has no duration`() {
        val hugeId3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0x7F, 0x7F, 0x7F, 0x7F)

        assertEquals(null, Mp3Duration.scan((hugeId3Header + ByteArray(500)).inputStream(), 510L))
    }

    @Test
    fun `a stream that is not an MP3 has no duration`() {
        assertEquals(null, Mp3Duration.scan(ByteArray(10_000).inputStream(), 10_000L))
    }

    /** An ID3v2.3 header, [tagBodySize] virtual zero bytes, then [audio]; counts how much of the tag is read. */
    private class PaddedId3Stream(private val tagBodySize: Int, private val audio: ByteArray) : InputStream() {
        private val header = byteArrayOf(
            'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0,
            (tagBodySize shr 21 and 0x7F).toByte(), (tagBodySize shr 14 and 0x7F).toByte(),
            (tagBodySize shr 7 and 0x7F).toByte(), (tagBodySize and 0x7F).toByte(),
        )
        private val total = header.size.toLong() + tagBodySize + audio.size
        private var position = 0L
        var tagBytesRead = 0L
            private set

        override fun read(): Int {
            val byte = ByteArray(1)
            return if (read(byte, 0, 1) < 0) -1 else byte[0].toInt() and 0xFF
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= total) return -1
            val count = minOf(length.toLong(), total - position).toInt()
            for (index in 0 until count) {
                val at = position + index
                val tagEnd = header.size.toLong() + tagBodySize
                buffer[offset + index] = when {
                    at < header.size -> header[at.toInt()]
                    at < tagEnd -> {
                        tagBytesRead++
                        0
                    }
                    else -> audio[(at - tagEnd).toInt()]
                }
            }
            position += count
            return count
        }

        override fun skip(count: Long): Long {
            val skipped = minOf(count, total - position).coerceAtLeast(0)
            position += skipped
            return skipped
        }
    }

    private companion object {
        const val ID3_HEADER_BYTES = 10
    }
}
