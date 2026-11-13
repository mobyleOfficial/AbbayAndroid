package com.mobyle.abbay.data.readalong

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import com.converter.AudioResampler
import com.converter.AudioResamplerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Decodes one of a book's files for the speech engine: from [offsetMs] into it, at most [lengthMs] (to its end when
 * null), as 16 kHz mono 16-bit samples, the format the speech models are trained on. Samples before the offset (a seek
 * lands on the frame before it) are dropped, so the first sample handed over is the one at the offset: the caller's
 * time is then just the count of samples it has received.
 *
 * Blocking; runs on the caller's thread and stops as soon as [isStopped] says so.
 */
class ReadAlongAudioDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resamplers: AudioResamplerFactory,
) {
    fun decode(uri: String, offsetMs: Long, lengthMs: Long?, isStopped: () -> Boolean, onSamples: (ShortArray) -> Unit) {
        ReadAlongExtractor.open(context, Uri.parse(uri)).use { extractor ->
            val offsetUs = offsetMs * MICROS_PER_MILLI
            if (offsetUs > 0) extractor.seekTo(offsetUs)
            val endUs = lengthMs?.let { offsetUs + it * MICROS_PER_MILLI }
            Pipeline(extractor, extractor.format, offsetUs, endUs, isStopped, onSamples).run()
        }
    }

    private inner class Pipeline(
        private val extractor: ReadAlongExtractor,
        format: MediaFormat,
        private val offsetUs: Long,
        private val endUs: Long?,
        private val isStopped: () -> Boolean,
        private val onSamples: (ShortArray) -> Unit,
    ) {
        private val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        private val info = MediaCodec.BufferInfo()
        private var rate = format.getIntegerOr(MediaFormat.KEY_SAMPLE_RATE, SPEECH_RATE)
        private var channels = format.getIntegerOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
        private var isFloat = false
        private var resampler: AudioResampler = resamplers.create(rate, SPEECH_RATE, 1, PASSBAND)
        private var isExtractorDone = false
        private var isDecoderDone = false
        private var isPastEnd = false

        init {
            decoder.configure(format, null, null, 0)
            decoder.start()
        }

        fun run() {
            try {
                while (!isDecoderDone && !isPastEnd) {
                    if (isStopped()) return
                    val fed = !isExtractorDone && feed()
                    val drained = drain()
                    if (!fed && !drained) Thread.sleep(IDLE_SLEEP_MS)
                }
                emit(resampler.flush())
            } finally {
                runCatching { decoder.stop() }
                decoder.release()
            }
        }

        private fun feed(): Boolean {
            val index = decoder.dequeueInputBuffer(0L)
            val buffer = if (index >= 0) decoder.getInputBuffer(index) else null
            if (buffer == null) return false
            val size = extractor.readSampleData(buffer)
            if (size < 0 || (endUs != null && extractor.sampleTimeUs > endUs + LOOKAHEAD_US)) {
                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isExtractorDone = true
            } else {
                decoder.queueInputBuffer(index, 0, size, extractor.sampleTimeUs, 0)
                extractor.advance()
            }
            return true
        }

        private fun drain(): Boolean {
            val index = decoder.dequeueOutputBuffer(info, DRAIN_WAIT_US)
            return when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val output = decoder.outputFormat
                    rate = output.getIntegerOr(MediaFormat.KEY_SAMPLE_RATE, rate)
                    channels = output.getIntegerOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                    isFloat = output.getIntegerOr(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT) ==
                        AudioFormat.ENCODING_PCM_FLOAT
                    resampler = resamplers.create(rate, SPEECH_RATE, 1, PASSBAND)
                    true
                }
                index >= 0 -> {
                    val buffer = decoder.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) accept(buffer, info.offset, info.size, info.presentationTimeUs)
                    decoder.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) isDecoderDone = true
                    true
                }
                else -> false
            }
        }

        /** Mixes a decoded buffer to mono, drops what lies before the offset or after the end, and resamples the rest. */
        private fun accept(buffer: ByteBuffer, offset: Int, size: Int, presentationUs: Long) {
            buffer.position(offset)
            buffer.limit(offset + size)
            val ordered = buffer.slice().order(ByteOrder.nativeOrder())
            val sampleCount = if (isFloat) size / FLOAT_BYTES else size / SHORT_BYTES
            val frames = sampleCount / channels
            val samples = FloatArray(sampleCount)
            if (isFloat) {
                ordered.asFloatBuffer().get(samples)
            } else {
                val shorts = ShortArray(sampleCount).also { ordered.asShortBuffer().get(it) }
                for (index in 0 until sampleCount) samples[index] = shorts[index] / SHORT_SCALE
            }
            val usPerFrame = MICROS_PER_SECOND.toDouble() / rate
            val skip = ((offsetUs - presentationUs) / usPerFrame).roundToInt().coerceIn(0, frames)
            val keep = endUs?.let { end -> ((end - presentationUs) / usPerFrame).roundToInt().coerceIn(0, frames) } ?: frames
            if (endUs != null && keep < frames) isPastEnd = true
            if (keep <= skip) return
            val mono = FloatArray(keep - skip)
            for (frame in skip until keep) {
                var sum = 0f
                for (channel in 0 until channels) sum += samples[frame * channels + channel]
                mono[frame - skip] = sum / channels
            }
            emit(resampler.process(mono))
        }

        private fun emit(samples: FloatArray) {
            if (samples.isEmpty()) return
            onSamples(ShortArray(samples.size) { index -> (samples[index] * SHORT_SCALE).coerceIn(SHORT_MIN, SHORT_MAX).toInt().toShort() })
        }
    }

    private fun MediaFormat.getIntegerOr(key: String, default: Int): Int = if (containsKey(key)) getInteger(key) else default

    companion object {
        const val SPEECH_RATE = 16_000
        private const val PASSBAND = 0.9
        private const val MICROS_PER_MILLI = 1_000L
        private const val MICROS_PER_SECOND = 1_000_000L
        private const val LOOKAHEAD_US = 500_000L
        private const val DRAIN_WAIT_US = 2_000L
        private const val IDLE_SLEEP_MS = 2L
        private const val FLOAT_BYTES = 4
        private const val SHORT_BYTES = 2
        private const val SHORT_SCALE = 32_768f
        private const val SHORT_MIN = -32_768f
        private const val SHORT_MAX = 32_767f
    }
}
