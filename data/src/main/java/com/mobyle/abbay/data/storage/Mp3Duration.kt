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
import java.io.SequenceInputStream

@OptIn(UnstableApi::class)
object Mp3Duration {
    private val headerMarkers = listOf("Xing", "Info", "VBRI").map { it.toByteArray(Charsets.US_ASCII) }
    private const val ID3_HEADER_SIZE = 10
    private val ID3_MAGIC = "ID3".toByteArray(Charsets.US_ASCII)
    private val ID3_SIZE_BYTES = 6..9
    private const val SYNCHSAFE_BITS = 7
    private const val SYNCHSAFE_MASK = 0x7F
    private const val FIRST_FRAME_SEARCH_BYTES = 2_048

    fun needsFullScan(stream: InputStream): Boolean {
        val header = stream.readUpTo(ID3_HEADER_SIZE)
        val tagSize = id3TagSize(header)
        val frameRegion = if (tagSize > 0) {
            if (!stream.skipFully(tagSize.toLong() - header.size)) return true
            stream.readUpTo(FIRST_FRAME_SEARCH_BYTES)
        } else {
            header + stream.readUpTo(FIRST_FRAME_SEARCH_BYTES - header.size)
        }
        return headerMarkers.none { marker -> indexOf(frameRegion, marker, 0, frameRegion.size) >= 0 }
    }

    fun scan(stream: InputStream, lengthBytes: Long): Long? {
        val audio = skipId3Tag(stream, lengthBytes) ?: return null
        val output = DurationOutput()
        val extractor = Mp3Extractor(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val reader = DataReader { buffer, offset, length ->
            val read = audio.stream.read(buffer, offset, length)
            if (read < 0) C.RESULT_END_OF_INPUT else read
        }
        val input = DefaultExtractorInput(reader, 0, audio.length)
        extractor.init(output)
        val positionHolder = PositionHolder()
        while (extractor.read(input, positionHolder) != Extractor.RESULT_END_OF_INPUT) Unit
        extractor.release()

        val durationUs = output.seekMap?.durationUs?.takeIf { it != C.TIME_UNSET } ?: return null
        return durationUs / MICROSECONDS_PER_MILLISECOND
    }

    /**
     * Mp3Extractor buffers the whole ID3 tag in memory, which runs out of heap on files with
     * large embedded covers or padding, so the extractor is handed the stream after the tag.
     */
    private fun skipId3Tag(stream: InputStream, lengthBytes: Long): AudioStream? {
        val header = stream.readUpTo(ID3_HEADER_SIZE)
        val tagSize = id3TagSize(header)
        val audioLength = if (lengthBytes > 0) lengthBytes - tagSize else C.LENGTH_UNSET.toLong()
        return when {
            tagSize == 0 -> AudioStream(SequenceInputStream(header.inputStream(), stream), audioLength)
            lengthBytes > 0 && audioLength <= 0 -> null
            stream.skipFully(tagSize.toLong() - header.size) -> AudioStream(stream, audioLength)
            else -> null
        }
    }

    private class AudioStream(val stream: InputStream, val length: Long)

    private fun id3TagSize(head: ByteArray): Int {
        val hasId3 = head.size >= ID3_HEADER_SIZE && head.copyOf(ID3_MAGIC.size).contentEquals(ID3_MAGIC)
        if (!hasId3) return 0
        val syncSafeSize = ID3_SIZE_BYTES.fold(0) { size, index -> (size shl SYNCHSAFE_BITS) or (head[index].toInt() and SYNCHSAFE_MASK) }
        return ID3_HEADER_SIZE + syncSafeSize
    }

    private fun indexOf(data: ByteArray, marker: ByteArray, start: Int, end: Int): Int {
        for (position in start..end - marker.size) {
            if (marker.indices.all { data[position + it] == marker[it] }) return position
        }
        return -1
    }

    private fun InputStream.readUpTo(count: Int): ByteArray {
        val buffer = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = read(buffer, filled, count - filled)
            if (read < 0) break
            filled += read
        }
        return buffer.copyOf(filled)
    }

    private fun InputStream.skipFully(count: Long): Boolean {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                if (read() < 0) return false
                remaining--
            }
        }
        return true
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
