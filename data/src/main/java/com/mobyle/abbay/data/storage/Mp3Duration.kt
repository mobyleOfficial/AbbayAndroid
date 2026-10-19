package com.mobyle.abbay.data.storage

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mp3.Mp3Extractor
import java.io.InputStream

/**
 * VBR MP3 files without a Xing/Info/VBRI header have no reliable duration in their headers; platform readers
 * extrapolate from the first frames and can be off by minutes. Their duration is computed by reading every frame
 * once. Pure logic over streams, unit-tested on the JVM.
 */
@OptIn(UnstableApi::class)
object Mp3Duration {
    private val headerMarkers = listOf("Xing", "Info", "VBRI").map { it.toByteArray(Charsets.US_ASCII) }
    private const val ID3_HEADER_SIZE = 10
    private val ID3_MAGIC = "ID3".toByteArray(Charsets.US_ASCII)
    private val ID3_SIZE_BYTES = 6..9
    private const val SYNCHSAFE_BITS = 7
    private const val SYNCHSAFE_MASK = 0x7F
    private const val FIRST_FRAME_SEARCH_BYTES = 2_048

    /** True when [head] (the first bytes of the file) shows no duration header after the ID3v2 tag. */
    fun needsFullScan(head: ByteArray): Boolean {
        val frameStart = id3TagSize(head)
        if (frameStart >= head.size) return true
        val searchEnd = minOf(head.size, frameStart + FIRST_FRAME_SEARCH_BYTES)
        return headerMarkers.none { marker -> indexOf(head, marker, frameStart, searchEnd) >= 0 }
    }

    /** Bytes of the file start needed by [needsFullScan], including a large ID3v2 tag. */
    fun headBytesNeeded(firstTenBytes: ByteArray): Int = id3TagSize(firstTenBytes) + FIRST_FRAME_SEARCH_BYTES

    /** Exact duration in milliseconds, reading the whole stream; null when it is not a readable MP3. */
    fun scan(stream: InputStream, lengthBytes: Long): Long? {
        val output = DurationOutput()
        val extractor = Mp3Extractor(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val reader = DataReader { buffer, offset, length ->
            val read = stream.read(buffer, offset, length)
            if (read < 0) C.RESULT_END_OF_INPUT else read
        }
        val input = DefaultExtractorInput(reader, 0, if (lengthBytes > 0) lengthBytes else C.LENGTH_UNSET.toLong())
        extractor.init(output)
        val positionHolder = PositionHolder()
        while (extractor.read(input, positionHolder) != Extractor.RESULT_END_OF_INPUT) Unit
        extractor.release()

        val durationUs = output.seekMap?.durationUs?.takeIf { it != C.TIME_UNSET } ?: return null
        return durationUs / MICROSECONDS_PER_MILLISECOND
    }

    private fun id3TagSize(head: ByteArray): Int {
        val hasId3 = head.size >= ID3_HEADER_SIZE && head.copyOf(ID3_MAGIC.size).contentEquals(ID3_MAGIC)
        if (!hasId3) return 0
        // ID3v2 size: 4 "synchsafe" bytes of 7 bits each (ID3v2.4 §3.1).
        val syncSafeSize = ID3_SIZE_BYTES.fold(0) { size, index -> (size shl SYNCHSAFE_BITS) or (head[index].toInt() and SYNCHSAFE_MASK) }
        return ID3_HEADER_SIZE + syncSafeSize
    }

    private fun indexOf(data: ByteArray, marker: ByteArray, start: Int, end: Int): Int {
        for (position in start..end - marker.size) {
            if (marker.indices.all { data[position + it] == marker[it] }) return position
        }
        return -1
    }

    private class DurationOutput : ExtractorOutput, TrackOutput {
        var seekMap: SeekMap? = null
        private val scratch = ByteArray(4_096)

        override fun track(id: Int, type: Int): TrackOutput = this

        override fun endTracks() = Unit

        override fun seekMap(seekMap: SeekMap) {
            this.seekMap = seekMap
        }

        override fun format(format: Format) = Unit

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int =
            input.read(scratch, 0, minOf(length, scratch.size))

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) = data.skipBytes(length)

        override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) = Unit
    }

    private const val MICROSECONDS_PER_MILLISECOND = 1_000L
}
