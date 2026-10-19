package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Base of the project's processors: accepts 16-bit and float PCM, works in float,
 * outputs the input encoding. The processor stays configured for the whole stream, because Media3 decides which
 * processors run only when the format is configured; while its effect is off, [isBypassed] makes it copy the input
 * unchanged, bit for bit. The float buffer is allocated in [onConfigure] and only grows for unusually large
 * input buffers.
 */
@OptIn(UnstableApi::class)
abstract class FloatPcmProcessor : BaseAudioProcessor() {
    protected var channelCount = 0
        private set
    protected var sampleRate = 0
        private set
    private var encoding = C.ENCODING_INVALID
    protected var samples = FloatArray(0)
        private set
    private var shorts = ShortArray(0)

    final override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        channelCount = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        encoding = inputAudioFormat.encoding
        samples = FloatArray(sampleRate * channelCount / INITIAL_BUFFERS_PER_SECOND)
        shorts = ShortArray(samples.size)
        onFormat()
        return inputAudioFormat
    }

    /** The format is known: create the DSP objects. */
    protected abstract fun onFormat()

    /** True when the effect does nothing now; the input is copied unchanged. */
    protected abstract fun isBypassed(): Boolean

    /** Processes [frameCount] interleaved frames of [samples] in place. */
    protected abstract fun process(samples: FloatArray, frameCount: Int)

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val output = replaceOutputBuffer(bytes)
        if (isBypassed()) {
            output.put(inputBuffer)
        } else {
            val sampleCount = read(inputBuffer)
            process(samples, sampleCount / channelCount)
            write(samples, sampleCount, output)
        }
        output.flip()
    }

    /** Reads the buffer into [samples]; returns the number of samples. */
    protected fun read(inputBuffer: ByteBuffer): Int {
        val buffer = inputBuffer.order(ByteOrder.nativeOrder())
        val sampleCount = if (encoding == C.ENCODING_PCM_FLOAT) buffer.remaining() / FLOAT_BYTES else buffer.remaining() / SHORT_BYTES
        if (samples.size < sampleCount) {
            samples = FloatArray(sampleCount)
            shorts = ShortArray(sampleCount)
        }
        // Bulk transfers: element-by-element ByteBuffer access is several times slower.
        if (encoding == C.ENCODING_PCM_FLOAT) {
            buffer.asFloatBuffer().get(samples, 0, sampleCount)
        } else {
            buffer.asShortBuffer().get(shorts, 0, sampleCount)
            for (index in 0 until sampleCount) samples[index] = shorts[index] / SHORT_SCALE
        }
        buffer.position(buffer.position() + bytesFor(sampleCount))
        return sampleCount
    }

    protected fun write(source: FloatArray, sampleCount: Int, output: ByteBuffer) {
        output.order(ByteOrder.nativeOrder())
        if (encoding == C.ENCODING_PCM_FLOAT) {
            output.asFloatBuffer().put(source, 0, sampleCount)
        } else {
            if (shorts.size < sampleCount) shorts = ShortArray(sampleCount)
            for (index in 0 until sampleCount) {
                shorts[index] = (source[index] * SHORT_SCALE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            output.asShortBuffer().put(shorts, 0, sampleCount)
        }
        output.position(output.position() + bytesFor(sampleCount))
    }

    protected fun bytesFor(sampleCount: Int): Int = sampleCount * if (encoding == C.ENCODING_PCM_FLOAT) FLOAT_BYTES else SHORT_BYTES

    private companion object {
        const val FLOAT_BYTES = 4
        const val SHORT_BYTES = 2
        const val SHORT_SCALE = 32_768f
        const val INITIAL_BUFFERS_PER_SECOND = 10
    }
}
