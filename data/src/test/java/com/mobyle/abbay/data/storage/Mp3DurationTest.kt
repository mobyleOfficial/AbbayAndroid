package com.mobyle.abbay.data.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Mp3DurationTest {
    private fun fixture(name: String) = File(requireNotNull(javaClass.classLoader?.getResource(name)).toURI())

    @Test
    fun `a file without a duration header needs a full scan and gets its exact duration`() {
        val file = fixture("vbr_no_xing.mp3")

        assertTrue(Mp3Duration.needsFullScan(file.readBytes().copyOf(4_096)))
        assertEquals(60_000.0, file.inputStream().use { Mp3Duration.scan(it.buffered(), file.length()) }!!.toDouble(), 100.0)
    }

    @Test
    fun `a file with a Xing header keeps the header duration`() {
        val head = fixture("vbr_with_xing.mp3").readBytes()

        assertFalse(Mp3Duration.needsFullScan(head.copyOf(Mp3Duration.headBytesNeeded(head.copyOf(10)))))
    }

    @Test
    fun `the ID3 tag is skipped before looking for the header`() {
        val id3Body = ByteArray(300) { 'X'.code.toByte() }
        val id3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 2, 44)
        val frame = ByteArray(100) + "Info".toByteArray() + ByteArray(100)

        assertEquals(10 + 300 + 2_048, Mp3Duration.headBytesNeeded(id3Header))
        assertFalse(Mp3Duration.needsFullScan(id3Header + id3Body + frame))
        assertTrue(Mp3Duration.needsFullScan(id3Header + "Xing".toByteArray() + ByteArray(296) + ByteArray(200)))
    }

    @Test
    fun `a stream that is not an MP3 has no duration`() {
        assertEquals(null, Mp3Duration.scan(ByteArray(10_000).inputStream(), 10_000L))
    }
}
