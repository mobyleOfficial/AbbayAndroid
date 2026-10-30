package com.mobyle.abbay.infra.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NoiseReducerTest {
    /** Stands in for RNNoise: passes audio through, one frame late, like the real one. */
    private class DelayingDenoiser(private val gain: Float = 1f) : FrameDenoiser {
        override val frameSize = 480
        private val previous = FloatArray(frameSize)

        override fun process(input: FloatArray, output: FloatArray) {
            for (index in 0 until frameSize) output[index] = previous[index] * gain
            input.copyInto(previous)
        }

        override fun close() = Unit
    }

    @Test
    fun at48kHzTheDelayIsExactly20MsAndBothPathsLineUp() {
        val reducer = NoiseReducer(48_000, 2, ::DelayingDenoiser).apply { setWet(0.5f, immediately = true) }
        assertEquals(960, reducer.latencyFrames)
        val input = Signals.sine(440.0, 1.0, 0.5f, channels = 2)
        val output = runInChunks(reducer, input, channels = 2, seed = 1)
        // Wet and dry are the same signal here, so a correct alignment gives the input back, delayed.
        for (frame in 2_000 until 40_000) {
            assertEquals(input[(frame - 960) * 2].toDouble(), output[frame * 2].toDouble(), 1e-5)
        }
    }

    @Test
    fun at44100HzTheResampledPathLinesUpWithTheDryOne() {
        val reducer = NoiseReducer(44_100, 1, ::DelayingDenoiser).apply { setWet(0.5f, immediately = true) }
        val latency = reducer.latencyFrames
        assertTrue("latency $latency", latency in 900..1_100)
        val input = Signals.sine(1_000.0, 1.0, 0.5f, rate = 44_100)
        val output = runInChunks(reducer, input, channels = 1, seed = 2)
        var errorEnergy = 0.0
        var signalEnergy = 0.0
        for (frame in 4_000 until 40_000) {
            val error = output[frame] - input[frame - latency]
            errorEnergy += error * error
            signalEnergy += input[frame - latency] * input[frame - latency]
        }
        assertEquals(0L, reducer.underflowCount)
        assertTrue("misalignment ${10 * kotlin.math.log10(errorEnergy / signalEnergy)} dB", errorEnergy / signalEnergy < 1e-4)
    }

    @Test
    fun strengthMixesTheDenoisedSignalWithTheOriginal() {
        // A denoiser that silences everything shows the dry share directly.
        val reducer = NoiseReducer(48_000, 1, { DelayingDenoiser(gain = 0f) }).apply { setWet(0.75f, immediately = true) }
        val input = FloatArray(48_000) { 0.4f }
        val output = runInChunks(reducer, input, channels = 1, seed = 3)
        assertEquals(0.1, output[10_000].toDouble(), 1e-5)
    }

    @Test
    fun identicalChannelsAreDenoisedOnceUntilTheyDiffer() {
        var denoisers = 0
        var framesDenoised = 0
        val counting = {
            denoisers++
            object : FrameDenoiser {
                override val frameSize = 480

                override fun process(input: FloatArray, output: FloatArray) {
                    framesDenoised++
                    input.copyInto(output)
                }

                override fun close() = Unit
            }
        }
        val reducer = NoiseReducer(48_000, 2, counting)
        val dualMono = FloatArray(4_800 * 2) { index -> (index / 2) * 1e-4f }
        reducer.process(dualMono, 4_800)
        assertEquals(10, framesDenoised)
        val stereo = FloatArray(4_800 * 2) { index -> if (index % 2 == 0) 0.1f else -0.1f }
        reducer.process(stereo, 4_800)
        assertEquals(30, framesDenoised)
        assertEquals(2, denoisers)
    }

    @Test
    fun outputLengthAlwaysMatchesTheInput() {
        val reducer = NoiseReducer(22_050, 1, ::DelayingDenoiser)
        val random = Random(4)
        repeat(200) {
            val frames = 1 + random.nextInt(3_000)
            val buffer = FloatArray(frames) { 0.1f }
            reducer.process(buffer, frames)
        }
        assertEquals(0L, reducer.underflowCount)
    }

    private fun runInChunks(reducer: NoiseReducer, input: FloatArray, channels: Int, seed: Int): FloatArray {
        val random = Random(seed)
        val output = input.copyOf()
        var frame = 0
        val totalFrames = input.size / channels
        while (frame < totalFrames) {
            val frames = minOf(totalFrames - frame, 1 + random.nextInt(2_048))
            val chunk = output.copyOfRange(frame * channels, (frame + frames) * channels)
            reducer.process(chunk, frames)
            chunk.copyInto(output, frame * channels)
            frame += frames
        }
        return output
    }
}
