package com.mobyle.abbay.data.converter

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.converter.AudioResamplerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.random.Random

class OpusTranscoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resamplers: AudioResamplerFactory,
) {
    class Result(val decodedDurationUs: Long, val inputSamples48k: Long)

    fun encoderName(): String? = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        .firstOrNull { it.isEncoder && MediaFormat.MIMETYPE_AUDIO_OPUS in it.supportedTypes }?.name

    @Suppress("LongParameterList")
    fun transcode(
        sourceUri: String,
        output: OutputStream,
        bitrateKbps: Int,
        forceMono: Boolean,
        tags: OpusTags,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean,
    ): Result {
        val extractor = MediaExtractor().apply { setDataSource(context, Uri.parse(sourceUri), null) }
        try {
            val trackIndex = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            extractor.selectTrack(trackIndex)
            val sourceFormat = extractor.getTrackFormat(trackIndex)
            val durationUs = if (sourceFormat.containsKey(MediaFormat.KEY_DURATION)) sourceFormat.getLong(MediaFormat.KEY_DURATION) else 0L
            val sourceChannels = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val outputChannels = if (forceMono || sourceChannels == 1) 1 else 2
            return Pipeline(
                extractor, sourceFormat, outputChannels, bitrateKbps, output, tags, durationUs, onProgress, isCancelled, resamplers,
            ).run()
        } finally {
            extractor.release()
        }
    }

    @Suppress("LongParameterList", "TooManyFunctions")
    private class Pipeline(
        private val extractor: MediaExtractor,
        sourceFormat: MediaFormat,
        private val outputChannels: Int,
        bitrateKbps: Int,
        private val output: OutputStream,
        private val tags: OpusTags,
        private val durationUs: Long,
        private val onProgress: (Float) -> Unit,
        private val isCancelled: () -> Boolean,
        private val resamplers: AudioResamplerFactory,
    ) {
        private val decoder = MediaCodec.createDecoderByType(sourceFormat.getString(MediaFormat.KEY_MIME)!!)
        private val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        private val info = MediaCodec.BufferInfo()
        private var decodedRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        private var decodedChannels = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        private var decodedIsFloat = false
        private var resampler = resamplers.create(decodedRate, OPUS_RATE, outputChannels, CONVERTER_PASSBAND)
        private val sourceRate = decodedRate

        private var pending = ShortArray(OPUS_RATE * outputChannels * 2)
        private var pendingStart = 0
        private var pendingEnd = 0
        private val pendingCount get() = pendingEnd - pendingStart
        private var encodedFrames = 0L
        private var inputSamples48k = 0L
        private var writer: OggOpusWriter? = null
        private var isExtractorDone = false
        private var isDecoderDone = false
        private var isEncoderInputDone = false
        private var isEncoderDone = false

        init {
            decoder.configure(sourceFormat, null, null, 0)
            val encoderFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, OPUS_RATE, outputChannels).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * BITS_PER_KILOBIT)
                setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            }
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            decoder.start()
            encoder.start()
        }

        fun run(): Result {
            try {
                while (!isEncoderDone) {
                    if (isCancelled()) throw InterruptedException("Conversion cancelled")
                    val isEncoderBehind = pendingCount > OPUS_RATE * outputChannels
                    var progressed = !isExtractorDone && !isEncoderBehind && feedDecoder()
                    progressed = (!isDecoderDone && !isEncoderBehind && drainDecoder()) || progressed
                    progressed = (!isEncoderInputDone && feedEncoder()) || progressed
                    progressed = drainEncoder(if (progressed) 0L else IDLE_WAIT_US) || progressed
                }
                writer?.finish(inputSamples48k) ?: error("The encoder produced no stream")
                return Result(inputSamples48k * MICROSECONDS_PER_SECOND / OPUS_RATE, inputSamples48k)
            } finally {
                runCatching { decoder.stop() }
                runCatching { encoder.stop() }
                decoder.release()
                encoder.release()
            }
        }

        private fun feedDecoder(): Boolean {
            val index = decoder.dequeueInputBuffer(0L)
            if (index < 0) return false
            val buffer = decoder.getInputBuffer(index)!!
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) {
                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isExtractorDone = true
            } else {
                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                if (durationUs > 0) onProgress((extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 1f))
                extractor.advance()
            }
            return true
        }

        private fun drainDecoder(): Boolean {
            val index = decoder.dequeueOutputBuffer(info, 0L)
            return when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    onDecodedFormat(decoder.outputFormat)
                    true
                }

                index >= 0 -> {
                    val buffer = decoder.getOutputBuffer(index)!!
                    if (info.size > 0) accept(buffer, info.offset, info.size)
                    decoder.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isDecoderDone = true
                        append(resampler.flush())
                    }
                    true
                }

                else -> false
            }
        }

        private fun onDecodedFormat(format: MediaFormat) {
            decodedRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            decodedChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            decodedIsFloat = format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
            resampler = resamplers.create(decodedRate, OPUS_RATE, outputChannels, CONVERTER_PASSBAND)
        }

        private fun accept(buffer: ByteBuffer, offset: Int, size: Int) {
            buffer.position(offset)
            buffer.limit(offset + size)
            val ordered = buffer.order(ByteOrder.nativeOrder())
            val sampleCount = if (decodedIsFloat) size / FLOAT_BYTES else size / SHORT_BYTES
            val frames = sampleCount / decodedChannels
            val mixed = FloatArray(frames * outputChannels)
            val samples = FloatArray(sampleCount)
            if (decodedIsFloat) {
                ordered.asFloatBuffer().get(samples)
            } else {
                val shorts = ShortArray(sampleCount).also { ordered.asShortBuffer().get(it) }
                for (index in 0 until sampleCount) samples[index] = shorts[index] / SHORT_SCALE
            }
            for (frame in 0 until frames) {
                if (outputChannels == 1) {
                    var sum = 0f
                    for (channel in 0 until decodedChannels) sum += samples[frame * decodedChannels + channel]
                    mixed[frame] = sum / decodedChannels
                } else {
                    mixed[frame * 2] = samples[frame * decodedChannels]
                    mixed[frame * 2 + 1] = samples[frame * decodedChannels + minOf(1, decodedChannels - 1)]
                }
            }
            append(resampler.process(mixed))
        }

        private fun append(samples48k: FloatArray) {
            if (pendingEnd + samples48k.size > pending.size) {
                pending.copyInto(pending, 0, pendingStart, pendingEnd)
                pendingEnd -= pendingStart
                pendingStart = 0
                val needed = pendingEnd + samples48k.size
                if (needed > pending.size) pending = pending.copyOf(maxOf(pending.size * 2, needed))
            }
            for (sample in samples48k) {
                pending[pendingEnd++] = (sample.coerceIn(-1f, 1f) * SHORT_MAXIMUM).toInt().toShort()
            }
            inputSamples48k += samples48k.size / outputChannels
        }

        private fun feedEncoder(): Boolean {
            if (pendingCount == 0 && !isDecoderDone) return false
            val index = encoder.dequeueInputBuffer(0L)
            if (index < 0) return false
            val buffer = encoder.getInputBuffer(index)!!.order(ByteOrder.nativeOrder())
            val timeUs = encodedFrames * MICROSECONDS_PER_SECOND / OPUS_RATE
            if (pendingCount == 0) {
                encoder.queueInputBuffer(index, 0, 0, timeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isEncoderInputDone = true
            } else {
                val count = minOf(pendingCount, buffer.capacity() / SHORT_BYTES / outputChannels * outputChannels)
                buffer.asShortBuffer().put(pending, pendingStart, count)
                encoder.queueInputBuffer(index, 0, count * SHORT_BYTES, timeUs, 0)
                pendingStart += count
                encodedFrames += count / outputChannels
            }
            return true
        }

        private fun drainEncoder(waitUs: Long): Boolean {
            val index = encoder.dequeueOutputBuffer(info, waitUs)
            if (index < 0) return false
            val buffer = encoder.getOutputBuffer(index)!!
            val bytes = ByteArray(info.size).also {
                buffer.position(info.offset)
                buffer.get(it)
            }
            encoder.releaseOutputBuffer(index, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                startStream(bytes)
            } else if (bytes.isNotEmpty()) {
                (writer ?: startStream(ByteArray(0))).writeAudio(bytes)
            }
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) isEncoderDone = true
            return true
        }

        private fun startStream(codecConfig: ByteArray): OggOpusWriter {
            writer?.let { return it }
            val headAt = indexOf(codecConfig, OPUS_HEAD)
            val preSkip = if (headAt >= 0 && headAt + PRE_SKIP_OFFSET + 1 < codecConfig.size) {
                (codecConfig[headAt + PRE_SKIP_OFFSET].toInt() and BYTE_MASK) or
                    ((codecConfig[headAt + PRE_SKIP_OFFSET + 1].toInt() and BYTE_MASK) shl BITS_PER_BYTE)
            } else {
                DEFAULT_PRE_SKIP
            }
            return OggOpusWriter(output, outputChannels, sourceRate, preSkip, Random.nextInt()).also {
                it.writeHeaders(tags)
                writer = it
            }
        }

        private fun indexOf(bytes: ByteArray, pattern: ByteArray): Int =
            (0..bytes.size - pattern.size).firstOrNull { start -> pattern.indices.all { bytes[start + it] == pattern[it] } } ?: -1
    }

    private companion object {
        const val OPUS_RATE = 48_000
        const val IDLE_WAIT_US = 2_000L

        const val CONVERTER_PASSBAND = 0.8
        const val BITS_PER_KILOBIT = 1_000
        const val MICROSECONDS_PER_SECOND = 1_000_000L
        const val FLOAT_BYTES = 4
        const val SHORT_BYTES = 2
        const val SHORT_SCALE = 32_768f
        const val SHORT_MAXIMUM = 32_767f
        const val PRE_SKIP_OFFSET = 10
        const val DEFAULT_PRE_SKIP = 312
        const val BYTE_MASK = 0xFF
        const val BITS_PER_BYTE = 8
        val OPUS_HEAD = "OpusHead".toByteArray(Charsets.US_ASCII)
    }
}
