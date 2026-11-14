package com.mobyle.abbay.data.converter

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Base64

data class OpusTags(
    val comments: List<Pair<String, String>>,
    val cover: Cover? = null,
) {
    class Cover(val mimeType: String, val data: ByteArray)
}

@Suppress("MagicNumber", "TooManyFunctions")
class OggOpusWriter(
    private val output: OutputStream,
    private val channels: Int,
    private val inputSampleRate: Int,
    private val preSkip: Int,
    private val serialNumber: Int,
) {
    private var pageSequence = 0
    private val pagePackets = ArrayList<ByteArray>()
    private var pageSamples = 0
    private var granule = 0L
    private var hasWrittenHeaders = false

    fun writeHeaders(tags: OpusTags, vendor: String = VENDOR) {
        check(!hasWrittenHeaders)
        hasWrittenHeaders = true
        writePacketPages(head(), granule = 0, isFirst = true, isLast = false)
        writePacketPages(tags(tags, vendor), granule = 0, isFirst = false, isLast = false)
    }

    fun writeAudio(packet: ByteArray) {
        check(hasWrittenHeaders)
        pagePackets += packet
        val samples = OpusPackets.samplesOf(packet)
        pageSamples += samples
        granule += samples
        if (pageSamples >= SAMPLES_PER_PAGE || segmentsOf(pagePackets) > MAXIMUM_SEGMENTS - MAXIMUM_PACKET_SEGMENTS) flushAudioPage(false)
    }

    fun finish(totalInputSamples48k: Long) {
        val endGranule = minOf(granule, preSkip + totalInputSamples48k)
        granule = endGranule
        flushAudioPage(isLast = true)
        output.flush()
    }

    private fun flushAudioPage(isLast: Boolean) {
        if (pagePackets.isEmpty() && !isLast) return
        writePage(pagePackets.toList(), lastPacketComplete = true, granule = granule, flags = if (isLast) FLAG_LAST else 0)
        pagePackets.clear()
        pageSamples = 0
    }

    private fun writePacketPages(packet: ByteArray, granule: Long, isFirst: Boolean, isLast: Boolean) {
        var offset = 0
        var isContinued = false
        do {
            val chunkLength = minOf(packet.size - offset, MAXIMUM_SEGMENTS * SEGMENT_SIZE)
            val chunk = packet.copyOfRange(offset, offset + chunkLength)
            offset += chunkLength
            val endsHere = offset == packet.size && chunkLength < MAXIMUM_SEGMENTS * SEGMENT_SIZE
            var flags = if (isContinued) FLAG_CONTINUED else 0
            if (isFirst && pageSequence == 0) flags = flags or FLAG_FIRST
            if (isLast && endsHere) flags = flags or FLAG_LAST
            writePage(listOf(chunk), lastPacketComplete = endsHere, granule = if (endsHere) granule else NO_GRANULE, flags = flags)
            isContinued = !endsHere
        } while (!(offset == packet.size && !isContinued))
    }

    private fun writePage(packets: List<ByteArray>, lastPacketComplete: Boolean, granule: Long, flags: Int) {
        val segments = ArrayList<Int>()
        packets.forEachIndexed { index, packet ->
            var remaining = packet.size
            while (remaining >= SEGMENT_SIZE) {
                segments += SEGMENT_SIZE
                remaining -= SEGMENT_SIZE
            }
            if (index < packets.lastIndex || lastPacketComplete) segments += remaining
        }
        val body = ByteArrayOutputStream()
        packets.forEach(body::write)
        val header = ByteArrayOutputStream()
        header.write(CAPTURE_PATTERN)
        header.write(0)
        header.write(flags)
        writeLittleEndian(header, granule, 8)
        writeLittleEndian(header, serialNumber.toLong(), 4)
        writeLittleEndian(header, (pageSequence++).toLong(), 4)
        writeLittleEndian(header, 0, 4)
        header.write(segments.size)
        segments.forEach(header::write)
        val page = header.toByteArray() + body.toByteArray()
        val crc = OggCrc.of(page)
        for (index in 0 until 4) page[CRC_OFFSET + index] = (crc ushr (8 * index)).toByte()
        output.write(page)
    }

    private fun head(): ByteArray {
        val head = ByteArrayOutputStream()
        head.write("OpusHead".toByteArray(Charsets.US_ASCII))
        head.write(1)
        head.write(channels)
        writeLittleEndian(head, preSkip.toLong(), 2)
        writeLittleEndian(head, inputSampleRate.toLong(), 4)
        writeLittleEndian(head, 0, 2)
        head.write(0)
        return head.toByteArray()
    }

    private fun tags(tags: OpusTags, vendor: String): ByteArray {
        val comments = tags.comments.filter { (_, value) -> value.isNotBlank() }.map { (key, value) -> "$key=$value" } +
            listOfNotNull(tags.cover?.let { "METADATA_BLOCK_PICTURE=" + Base64.getEncoder().encodeToString(pictureBlock(it)) })
        val packet = ByteArrayOutputStream()
        packet.write("OpusTags".toByteArray(Charsets.US_ASCII))
        writeString(packet, vendor)
        writeLittleEndian(packet, comments.size.toLong(), 4)
        comments.forEach { writeString(packet, it) }
        return packet.toByteArray()
    }

    private fun pictureBlock(cover: OpusTags.Cover): ByteArray {
        val block = ByteArrayOutputStream()
        writeBigEndian(block, FRONT_COVER, 4)
        val mime = cover.mimeType.toByteArray(Charsets.US_ASCII)
        writeBigEndian(block, mime.size.toLong(), 4)
        block.write(mime)
        writeBigEndian(block, 0, 4)
        repeat(4) { writeBigEndian(block, 0, 4) }
        writeBigEndian(block, cover.data.size.toLong(), 4)
        block.write(cover.data)
        return block.toByteArray()
    }

    private fun writeString(stream: ByteArrayOutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        writeLittleEndian(stream, bytes.size.toLong(), 4)
        stream.write(bytes)
    }

    private fun segmentsOf(packets: List<ByteArray>): Int = packets.sumOf { it.size / SEGMENT_SIZE + 1 }

    private fun writeLittleEndian(stream: ByteArrayOutputStream, value: Long, bytes: Int) {
        for (index in 0 until bytes) stream.write(((value ushr (8 * index)) and BYTE_MASK).toInt())
    }

    private fun writeBigEndian(stream: ByteArrayOutputStream, value: Long, bytes: Int) {
        for (index in bytes - 1 downTo 0) stream.write(((value ushr (8 * index)) and BYTE_MASK).toInt())
    }

    companion object {
        const val VENDOR = "Abbay"
        const val SAMPLES_PER_PAGE = 48_000
        private const val SEGMENT_SIZE = 255
        private const val MAXIMUM_SEGMENTS = 255

        private const val MAXIMUM_PACKET_SEGMENTS = 31
        private const val FLAG_CONTINUED = 0x01
        private const val FLAG_FIRST = 0x02
        private const val FLAG_LAST = 0x04
        private const val CRC_OFFSET = 22
        private const val NO_GRANULE = -1L
        private const val FRONT_COVER = 3L
        private const val BYTE_MASK = 0xFFL
        private val CAPTURE_PATTERN = "OggS".toByteArray(Charsets.US_ASCII)
    }
}

object OpusPackets {
    private val silkFrames = intArrayOf(480, 960, 1_920, 2_880)
    private val hybridFrames = intArrayOf(480, 960)
    private val celtFrames = intArrayOf(120, 240, 480, 960)

    @Suppress("MagicNumber")
    fun samplesOf(packet: ByteArray): Int {
        if (packet.isEmpty()) return 0
        val toc = packet[0].toInt() and 0xFF
        val config = toc ushr 3
        val frameSamples = when {
            config < 12 -> silkFrames[config % 4]
            config < 16 -> hybridFrames[config % 2]
            else -> celtFrames[config % 4]
        }
        val frames = when (toc and 0x03) {
            0 -> 1
            1, 2 -> 2
            else -> if (packet.size > 1) packet[1].toInt() and 0x3F else 0
        }
        return frameSamples * frames
    }
}

object OggCrc {
    @Suppress("MagicNumber")
    private val table = IntArray(256) { index ->
        var value = index shl 24
        repeat(8) { value = if (value and 0x80000000.toInt() != 0) (value shl 1) xor 0x04C11DB7 else value shl 1 }
        value
    }

    @Suppress("MagicNumber")
    fun of(bytes: ByteArray): Int {
        var crc = 0
        for (byte in bytes) crc = (crc shl 8) xor table[((crc ushr 24) xor (byte.toInt() and 0xFF)) and 0xFF]
        return crc
    }
}
