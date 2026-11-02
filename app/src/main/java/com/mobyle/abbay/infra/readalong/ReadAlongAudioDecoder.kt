package com.mobyle.abbay.infra.readalong

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.mobyle.abbay.infra.dsp.Resampler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decodes a book's file to the one shape the recogniser takes: 16 kHz, one channel, signed 16-bit little-endian
 * PCM. It reads what is already on the device through the same content resolver the player uses, from a point in
 * the file to another, and hands the audio over in blocks so a chapter can be transcribed while it is decoded.
 */
@Singleton
@Suppress("TooManyFunctions")
class ReadAlongAudioDecoder @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Decodes [uri] from [startMs] to [endMs] of the file, giving each block of PCM to [onPcm]. A file that
     * isn't audio, or that the decoder refuses, throws [ReadAlongEngineException].
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "SwallowedException", "TooGenericExceptionCaught")
    suspend fun decode(uri: Uri, startMs: Long, endMs: Long, onPcm: suspend (ByteArray) -> Unit) = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = audioTrack(extractor)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw ReadAlongEngineException("The file has no audio format")
            extractor.selectTrack(trackIndex)
            if (startMs > 0) extractor.seekTo(startMs * MICROS_PER_MS, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            decoder = MediaCodec.createDecoderByType(mime).also {
                it.configure(format, null, null, 0)
                it.start()
            }
            decodeWith(extractor, decoder, startMs, endMs, format, onPcm)
        } catch (error: ReadAlongEngineException) {
            throw error
        } catch (error: Exception) {
            throw ReadAlongEngineException("Cannot read this file as audio", error)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            extractor.release()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList", "NestedBlockDepth")
    private suspend fun decodeWith(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        startMs: Long,
        endMs: Long,
        inputFormat: MediaFormat,
        onPcm: suspend (ByteArray) -> Unit,
    ) {
        var sampleRate = inputFormat.integerOr(MediaFormat.KEY_SAMPLE_RATE, DEFAULT_RATE)
        var channels = inputFormat.integerOr(MediaFormat.KEY_CHANNEL_COUNT, DEFAULT_CHANNELS)
        var resampler = Resampler(sampleRate, TARGET_RATE, 1)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var framesToSkip = -1L
        val startUs = startMs * MICROS_PER_MS
        val endUs = if (endMs > startMs) endMs * MICROS_PER_MS else Long.MAX_VALUE
        while (!outputDone) {
            currentCoroutineContext().ensureActive()
            if (!inputDone) inputDone = feed(extractor, decoder)
            val index = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val output = decoder.outputFormat
                    val rate = output.integerOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                    val count = output.integerOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                    if (rate != sampleRate || count != channels) {
                        sampleRate = rate
                        channels = count
                        resampler = Resampler(sampleRate, TARGET_RATE, 1)
                    }
                }
                index >= 0 -> {
                    val buffer = decoder.getOutputBuffer(index)
                    if (buffer != null && info.size > 0 && info.presentationTimeUs >= 0) {
                        if (framesToSkip < 0) framesToSkip = framesBefore(info.presentationTimeUs, startUs, sampleRate)
                        val block = buffer.toMonoFrames(info, channels, sampleRate, framesToSkip, endUs)
                        if (block.frameCount > 0) {
                            framesToSkip = (framesToSkip - block.skippedFrames).coerceAtLeast(0L)
                            emitResampled(resampler, block.floats, block.frameCount, onPcm)
                        } else {
                            framesToSkip = (framesToSkip - block.skippedFrames).coerceAtLeast(0L)
                        }
                        if (info.presentationTimeUs + framesToMicros(block.frameCount, sampleRate) >= endUs) outputDone = true
                    }
                    decoder.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        emitTail(resampler, onPcm)
    }

    @Suppress("ReturnCount")
    private fun feed(extractor: MediaExtractor, decoder: MediaCodec): Boolean {
        val index = decoder.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false
        val buffer = decoder.getInputBuffer(index) ?: return false
        val size = extractor.readSampleData(buffer, 0)
        return if (size < 0) {
            decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            true
        } else {
            decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
            extractor.advance()
            false
        }
    }

    /** One decoded buffer as mono floats, having dropped the frames that belong before the run's start. */
    private class MonoBlock(val floats: FloatArray, val frameCount: Int, val skippedFrames: Long)

    private fun ByteBuffer.toMonoFrames(
        info: MediaCodec.BufferInfo,
        channels: Int,
        sampleRate: Int,
        framesToSkip: Long,
        endUs: Long,
    ): MonoBlock {
        val shorts = duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val available = minOf(shorts.remaining(), info.size / BYTES_PER_SAMPLE)
        val frames = available / channels
        val skip = minOf(framesToSkip, frames.toLong()).toInt()
        val remaining = frames - skip
        // Nothing past the end of the segment belongs to this run.
        val untilEnd = framesWithinUs(endUs - info.presentationTimeUs, sampleRate)
        val kept = minOf(remaining, untilEnd)
        val floats = FloatArray(kept)
        for (frame in 0 until kept) {
            var sum = 0f
            for (channel in 0 until channels) {
                sum += shorts.get((skip + frame) * channels + channel).toFloat()
            }
            floats[frame] = sum / channels / Short.MAX_VALUE
        }
        return MonoBlock(floats, kept, skip.toLong())
    }

    private suspend fun emitResampled(resampler: Resampler, floats: FloatArray, frames: Int, onPcm: suspend (ByteArray) -> Unit) {
        val produced = resampler.process(floats, 0, frames)
        deliver(resampler.output, produced, onPcm)
    }

    private suspend fun emitTail(resampler: Resampler, onPcm: suspend (ByteArray) -> Unit) {
        val tail = resampler.flush()
        deliver(tail, tail.size, onPcm)
    }

    private suspend fun deliver(floats: FloatArray, frames: Int, onPcm: suspend (ByteArray) -> Unit) {
        if (frames <= 0) return
        val bytes = ByteArray(frames * BYTES_PER_SAMPLE)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (frame in 0 until frames) {
            buffer.putShort((floats[frame].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        }
        onPcm(bytes)
    }

    private fun audioTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return index
        }
        throw ReadAlongEngineException("The file has no audio track")
    }

    private fun MediaFormat.integerOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private fun framesBefore(timeUs: Long, startUs: Long, sampleRate: Int): Long =
        ((startUs - timeUs) * sampleRate / MICROS_PER_SECOND).coerceAtLeast(0L)

    private fun framesWithinUs(durationUs: Long, sampleRate: Int): Int {
        if (durationUs >= Long.MAX_VALUE / 2) return Int.MAX_VALUE
        return (durationUs * sampleRate / MICROS_PER_SECOND).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun framesToMicros(frames: Int, sampleRate: Int): Long = frames.toLong() * MICROS_PER_SECOND / sampleRate

    private companion object {
        const val TARGET_RATE = 16_000
        const val DEFAULT_RATE = 44_100
        const val DEFAULT_CHANNELS = 2
        const val BYTES_PER_SAMPLE = 2
        const val MICROS_PER_MS = 1_000L
        const val MICROS_PER_SECOND = 1_000_000L
        const val TIMEOUT_US = 10_000L
    }
}
