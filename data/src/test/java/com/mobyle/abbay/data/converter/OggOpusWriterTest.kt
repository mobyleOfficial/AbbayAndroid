package com.mobyle.abbay.data.converter

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

class OggOpusWriterTest {
    private class Page(val flags: Int, val granule: Long, val sequence: Int, val segments: IntArray, val body: ByteArray)

    @Test
    fun headersAudioGranulesAndChecksumsFollowTheSpecifications() {
        val output = ByteArrayOutputStream()
        val writer = OggOpusWriter(output, channels = 1, inputSampleRate = 44_100, preSkip = 312, serialNumber = 7)
        writer.writeHeaders(OpusTags(listOf("TITLE" to "Capítulo 1", "ARTIST" to "")))
        repeat(120) { writer.writeAudio(byteArrayOf((31 shl 3).toByte(), 1, 2, 3)) }
        writer.finish(totalInputSamples48k = 114_000)

        val pages = parse(output.toByteArray())
        assertEquals(0x02, pages.first().flags)
        assertEquals("OpusHead", String(pages[0].body, 0, 8, Charsets.US_ASCII))
        val head = ByteBuffer.wrap(pages[0].body).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, pages[0].body[9].toInt())
        assertEquals(312, head.getShort(10).toInt())
        assertEquals(44_100, head.getInt(12))
        assertTrue(String(pages[1].body, Charsets.UTF_8).contains("TITLE=Capítulo 1"))
        assertTrue("empty tags are left out", !String(pages[1].body, Charsets.UTF_8).contains("ARTIST="))
        val audio = pages.drop(2)
        assertEquals(listOf(48_000L, 96_000L, 312L + 114_000L), audio.map { it.granule })
        assertEquals(0x04, audio.last().flags)
        assertEquals((0 until pages.size).toList(), pages.map { it.sequence })
    }

    @Test
    fun aLargeCoverSpansSeveralPagesAndComesBackWhole() {
        val cover = ByteArray(200_000) { (it * 31).toByte() }
        val output = ByteArrayOutputStream()
        val writer = OggOpusWriter(output, channels = 2, inputSampleRate = 48_000, preSkip = 312, serialNumber = 1)
        writer.writeHeaders(OpusTags(emptyList(), OpusTags.Cover("image/jpeg", cover)))
        writer.writeAudio(byteArrayOf((31 shl 3).toByte(), 0))
        writer.finish(960)

        val pages = parse(output.toByteArray())
        val tagPages = pages.drop(1).takeWhile { it.granule == -1L } + pages.drop(1).first { it.granule == 0L }
        assertTrue(tagPages.size > 1)
        assertTrue(tagPages.drop(1).all { it.flags and 0x01 != 0 })
        val tags = tagPages.fold(ByteArray(0)) { all, page -> all + page.body }
        val text = String(tags, Charsets.ISO_8859_1)
        val encoded = text.substringAfter("METADATA_BLOCK_PICTURE=")
        val block = Base64.getDecoder().decode(encoded.trimEnd('\u0000'))
        val buffer = ByteBuffer.wrap(block)
        assertEquals(3, buffer.int)
        val mime = ByteArray(buffer.int).also(buffer::get)
        assertEquals("image/jpeg", String(mime))
        buffer.position(buffer.position() + 4 + 16)
        val data = ByteArray(buffer.int).also(buffer::get)
        assertArrayEquals(cover, data)
    }

    @Test
    fun packetDurationsComeFromTheTocByte() {
        assertEquals(960, OpusPackets.samplesOf(byteArrayOf((31 shl 3).toByte())))
        assertEquals(1_920, OpusPackets.samplesOf(byteArrayOf(((31 shl 3) or 1).toByte())))
        assertEquals(2_880, OpusPackets.samplesOf(byteArrayOf((3 shl 3).toByte())))
        assertEquals(3 * 480, OpusPackets.samplesOf(byteArrayOf(((12 shl 3) or 3).toByte(), 3)))
    }

    private fun parse(bytes: ByteArray): List<Page> {
        val pages = mutableListOf<Page>()
        var offset = 0
        while (offset < bytes.size) {
            assertEquals("OggS", String(bytes, offset, 4, Charsets.US_ASCII))
            val header = ByteBuffer.wrap(bytes, offset, 27).order(ByteOrder.LITTLE_ENDIAN)
            val flags = bytes[offset + 5].toInt()
            val granule = header.getLong(offset + 6)
            val sequence = header.getInt(offset + 18)
            val crc = header.getInt(offset + 22)
            val segmentCount = bytes[offset + 26].toInt() and 0xFF
            val segments = IntArray(segmentCount) { bytes[offset + 27 + it].toInt() and 0xFF }
            val bodyLength = segments.sum()
            val pageLength = 27 + segmentCount + bodyLength
            val page = bytes.copyOfRange(offset, offset + pageLength)
            for (index in 22 until 26) page[index] = 0
            assertEquals(crc, OggCrc.of(page))
            pages += Page(flags, granule, sequence, segments, bytes.copyOfRange(offset + 27 + segmentCount, offset + pageLength))
            offset += pageLength
        }
        return pages
    }
}
