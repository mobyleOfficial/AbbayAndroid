package com.mobyle.abbay.data.storage

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Reads a file with ExoPlayer's own extractors, which is what decides whether a file is playable (`02` §2). Used
 * when the platform `MediaMetadataRetriever` returns nothing: some vendor builds (seen on a Samsung tablet, Android
 * 14) give no duration and no audio flag for valid M4B files whose `moov` box is at the end. Only the container
 * headers are read: the extractor seeks over the audio data and stops once the tracks and the seek map are known.
 * Pure logic over a [FileChannel], unit-tested on the JVM.
 */
@OptIn(UnstableApi::class)
object ExtractorAudioProbe {
    data class Result(val durationMs: Long, val hasAudio: Boolean, val tags: Map<String, String>)

    /** Bytes read before giving up on a file whose headers never complete (a damaged file). */
    private const val MAX_BYTES_READ = 32L * 1024 * 1024
    private const val MICROSECONDS_PER_MILLISECOND = 1_000L

    /** Null when no extractor recognizes the file. */
    fun probe(channel: FileChannel): Result? {
        val length = channel.size()
        audioExtractors().forEach { extractor ->
            try {
                if (sniff(extractor, channel, length)) return read(extractor, channel, length)
            } finally {
                extractor.release()
            }
        }
        return null
    }

    /** The containers of the accepted extensions (`02` §2); MP4 first, the audiobook format. */
    private fun audioExtractors(): List<Extractor> = listOf(
        Mp4Extractor(), Mp3Extractor(), AdtsExtractor(), OggExtractor(), FlacExtractor(), WavExtractor(),
        MatroskaExtractor(), AmrExtractor(),
    )

    @Suppress("SwallowedException", "TooGenericExceptionCaught") // A failing sniff only means "not this format".
    private fun sniff(extractor: Extractor, channel: FileChannel, length: Long): Boolean = try {
        extractor.sniff(inputAt(channel, 0, length, ByteCounter()))
    } catch (exception: Exception) {
        false
    }

    private fun read(extractor: Extractor, channel: FileChannel, length: Long): Result {
        val output = ProbeOutput()
        val counter = ByteCounter()
        val positionHolder = PositionHolder()
        var input = inputAt(channel, 0, length, counter)
        extractor.init(output)
        while (!output.isComplete && counter.bytesRead < MAX_BYTES_READ) {
            when (extractor.read(input, positionHolder)) {
                Extractor.RESULT_END_OF_INPUT -> break
                Extractor.RESULT_SEEK -> input = inputAt(channel, positionHolder.position, length, counter)
            }
        }
        val durationUs = output.seekMap?.durationUs?.takeIf { it != C.TIME_UNSET } ?: 0L
        return Result(durationUs / MICROSECONDS_PER_MILLISECOND, output.hasAudio, output.tags)
    }

    private fun inputAt(channel: FileChannel, position: Long, length: Long, counter: ByteCounter): ExtractorInput {
        channel.position(position)
        val reader = DataReader { buffer, offset, readLength ->
            val read = channel.read(ByteBuffer.wrap(buffer, offset, readLength))
            if (read < 0) C.RESULT_END_OF_INPUT else read.also { counter.bytesRead += it }
        }
        return DefaultExtractorInput(reader, position, length)
    }

    private class ByteCounter {
        var bytesRead = 0L
    }

    private class ProbeOutput : ExtractorOutput, TrackOutput {
        var seekMap: SeekMap? = null
        var hasAudio = false
        val tags = mutableMapOf<String, String>()
        private var tracksEnded = false
        private val scratch = ByteArray(4_096)

        val isComplete get() = tracksEnded && seekMap != null

        override fun track(id: Int, type: Int): TrackOutput = also { if (type == C.TRACK_TYPE_AUDIO) hasAudio = true }

        override fun endTracks() {
            tracksEnded = true
        }

        override fun seekMap(seekMap: SeekMap) {
            this.seekMap = seekMap
        }

        override fun format(format: Format) {
            val metadata = format.metadata ?: return
            for (index in 0 until metadata.length()) {
                val frame = metadata.get(index) as? TextInformationFrame ?: continue
                frame.values.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { tags.putIfAbsent(frame.id, it) }
            }
        }

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int =
            input.read(scratch, 0, minOf(length, scratch.size))

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) = data.skipBytes(length)

        override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) = Unit
    }
}
