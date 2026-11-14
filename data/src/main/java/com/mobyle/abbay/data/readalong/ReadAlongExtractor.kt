package com.mobyle.abbay.data.readalong

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.MediaExtractorCompat
import java.io.IOException
import java.nio.ByteBuffer

/**
 * A book file's audio track, opened for [ReadAlongAudioDecoder]: the platform's extractor first, then Media3's (the one
 * the player itself reads with) when the platform's finds no audio. Some vendor extractors give up on files the player
 * handles: Samsung's MP4 extractor reports no tracks at all in some large `.m4b` files.
 */
internal class ReadAlongExtractor private constructor(private val source: Source, val format: MediaFormat) : AutoCloseable {
    val sampleTimeUs: Long get() = source.sampleTimeUs

    fun seekTo(timeUs: Long) = source.seekTo(timeUs)

    /** The next sample's size, or a negative value once the track is over. */
    fun readSampleData(buffer: ByteBuffer): Int = source.readSampleData(buffer)

    fun advance() = source.advance()

    override fun close() = source.close()

    /** The calls the decoder makes, which both extractors offer under the same names but no shared type. */
    private interface Source : AutoCloseable {
        val trackCount: Int
        val sampleTimeUs: Long

        fun trackFormat(index: Int): MediaFormat

        fun selectTrack(index: Int)

        fun seekTo(timeUs: Long)

        fun readSampleData(buffer: ByteBuffer): Int

        fun advance()
    }

    private class Platform(context: Context, uri: Uri) : Source {
        private val extractor = MediaExtractor().apply {
            runCatching { setDataSource(context, uri, null) }.onFailure { release() }.getOrThrow()
        }
        override val trackCount get() = extractor.trackCount
        override val sampleTimeUs get() = extractor.sampleTime

        override fun trackFormat(index: Int): MediaFormat = extractor.getTrackFormat(index)

        override fun selectTrack(index: Int) = extractor.selectTrack(index)

        override fun seekTo(timeUs: Long) = extractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        override fun readSampleData(buffer: ByteBuffer) = extractor.readSampleData(buffer, 0)

        override fun advance() {
            extractor.advance()
        }

        override fun close() = extractor.release()
    }

    @OptIn(UnstableApi::class)
    private class Media3(context: Context, uri: Uri) : Source {
        private val extractor = MediaExtractorCompat(context).apply {
            runCatching { setDataSource(uri, 0L) }.onFailure { release() }.getOrThrow()
        }
        override val trackCount get() = extractor.trackCount
        override val sampleTimeUs get() = extractor.sampleTime

        override fun trackFormat(index: Int): MediaFormat = extractor.getTrackFormat(index)

        override fun selectTrack(index: Int) = extractor.selectTrack(index)

        override fun seekTo(timeUs: Long) = extractor.seekTo(timeUs, MediaExtractorCompat.SEEK_TO_PREVIOUS_SYNC)

        override fun readSampleData(buffer: ByteBuffer) = extractor.readSampleData(buffer, 0)

        override fun advance() {
            extractor.advance()
        }

        override fun close() = extractor.release()
    }

    companion object {
        /** Opens [uri]'s first audio track; throws when neither extractor finds one. */
        fun open(context: Context, uri: Uri): ReadAlongExtractor {
            val opened = runCatching { audio(Platform(context, uri)) }.getOrNull()
            return opened ?: audio(Media3(context, uri)) ?: throw IOException("No audio track in $uri")
        }

        /** The source with its first audio track selected, or null (and the source closed) when it has none. */
        private fun audio(source: Source): ReadAlongExtractor? {
            val opened = runCatching {
                val track = (0 until source.trackCount).firstOrNull { index ->
                    source.trackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: return@runCatching null
                source.selectTrack(track)
                ReadAlongExtractor(source, source.trackFormat(track))
            }.getOrNull()
            if (opened == null) source.close()
            return opened
        }
    }
}
