package com.mobyle.abbay.presentation.playback.effects

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.effects.AudioEffects
import com.effects.SilenceTrimLevel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class EffectProcessorsTest {
    private val rate = 44_100
    private val format = AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_16BIT)

    @Test
    fun withEffectsOffEveryProcessorIsBitExact() {
        val state = EffectsState()
        val input = tone(seconds = 0.5)
        listOf(SilenceTrimProcessor(state), EqualizerProcessor(state), ChannelProcessor(state)).forEach { processor ->
            assertArrayEquals(processor.javaClass.simpleName, input, run(processor, input))
        }
    }

    @Test
    fun loudnessIsACopyWithEffectsOffAndSwitchesWithoutAStep() {
        val state = EffectsState()
        val input = tone(seconds = 1.0, amplitude = 0.3)
        val processor = LoudnessProcessor(state)
        assertArrayEquals(input, run(processor, input))

        // Raising a band brings in the limiter mid-stream; the 3 ms delay arrives through a crossfade, not a jump.
        val output = runSwitchingHalfway(processor, input) {
            state.current =
                AudioEffects(equalizerGains = listOf(0, 0, 0, 0, 0, 3, 0, 0, 0, 0))
        }
        val inputStep = maximumStep(input)
        assertTrue(maximumStep(output) <= 2 * inputStep)
    }

    @Test
    fun silenceTrimReportsDroppedFramesForTheMediaPosition() {
        val state = EffectsState().apply { current = AudioEffects(silenceTrim = SilenceTrimLevel.MODERATE) }
        // 1 s tone, 2 s silence, 1 s tone, far from any protected edge.
        state.trackBoundaries = EffectsState.TrackBoundaries(durationUs = 600_000_000L, chapterEdgesUs = emptyList())
        state.currentBufferTrackTimeUs = 60_000_000L
        val input = tone(1.0) + ShortArray(2 * rate * 2) + tone(1.0)
        val processor = SilenceTrimProcessor(state)

        val output = run(processor, input)

        val droppedFrames = (input.size - output.size) / 2
        assertEquals(droppedFrames.toLong(), processor.skippedFrames)
        assertEquals(1.8, droppedFrames.toDouble() / rate, 0.011)
        assertTrue(state.takeSavedSilenceMs() in 1_790L..1_810L)
    }

    private fun run(processor: AudioProcessor, input: ShortArray): ShortArray {
        processor.configure(format)
        processor.flush()
        val output = ArrayList<Short>()
        input.toList().chunked(4_410).forEach { chunk ->
            val buffer = ByteBuffer.allocateDirect(chunk.size * 2).order(ByteOrder.nativeOrder())
            chunk.forEach(buffer::putShort)
            buffer.flip()
            processor.queueInput(buffer)
            drain(processor, output)
        }
        processor.queueEndOfStream()
        drain(processor, output)
        return output.toShortArray()
    }

    private fun runSwitchingHalfway(processor: AudioProcessor, input: ShortArray, change: () -> Unit): ShortArray {
        processor.configure(format)
        processor.flush()
        val output = ArrayList<Short>()
        input.toList().chunked(4_410).forEachIndexed { index, chunk ->
            if (index == 5) change()
            val buffer = ByteBuffer.allocateDirect(chunk.size * 2).order(ByteOrder.nativeOrder())
            chunk.forEach(buffer::putShort)
            buffer.flip()
            processor.queueInput(buffer)
            drain(processor, output)
        }
        return output.toShortArray()
    }

    /** Largest step between successive samples of the left channel. */
    private fun maximumStep(
        samples: ShortArray
    ): Int = (2 until samples.size step 2).maxOf { kotlin.math.abs(samples[it] - samples[it - 2]) }

    private fun drain(processor: AudioProcessor, output: MutableList<Short>) {
        val buffer = processor.output.order(ByteOrder.nativeOrder())
        while (buffer.remaining() >= 2) output += buffer.getShort()
    }

    private fun tone(seconds: Double, amplitude: Double = 0.5): ShortArray {
        val frames = (seconds * rate).toInt()
        return ShortArray(frames * 2) { index ->
            (amplitude * Short.MAX_VALUE * sin(2 * PI * 440.0 * (index / 2) / rate)).toInt().toShort()
        }
    }
}
