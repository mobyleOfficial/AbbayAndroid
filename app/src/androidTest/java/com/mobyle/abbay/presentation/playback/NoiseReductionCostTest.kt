package com.mobyle.abbay.presentation.playback

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobyle.abbay.infra.dsp.FrameDenoiser
import com.mobyle.abbay.infra.dsp.NoiseReducer
import com.mobyle.abbay.infra.dsp.Resampler
import com.mobyle.abbay.infra.dsp.nativelib.RnnNoise
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Processing time of noise reduction relative to real time (budget ≤ 8 % of a core at 1×). Logs the shares of
 * RNNoise and of the resamplers.
 */
@RunWith(AndroidJUnit4::class)
class NoiseReductionCostTest {
    private val seconds = 20

    @Test
    fun measureNoiseReductionCost() {
        val random = Random(1)
        repeat(2) { round ->
            for ((rate, channels, isDualMono) in listOf(
                Triple(48_000, 1, false),
                Triple(44_100, 1, false),
                Triple(44_100, 2, false),
                Triple(44_100, 2, true)
            )) {
                val noise = FloatArray(rate * seconds) { (random.nextFloat() * 2 - 1) * 0.1f }
                val input = FloatArray(rate * channels * seconds) { index ->
                    if (isDualMono) noise[index / channels] else (random.nextFloat() * 2 - 1) * 0.1f
                }
                val total = time { process(NoiseReducer(rate, channels) { RealDenoiser() }, input, rate, channels) }
                val denoiserOnly = time { denoiseOnly(input.size / channels, channels) }
                val resamplersOnly = if (rate == 48_000) 0.0 else time { resampleOnly(input, rate, channels) }
                if (round == 1) {
                    Log.i(
                        TAG,
                        "rate=$rate channels=$channels dualMono=$isDualMono total=${percent(total)}% rnnoise=${percent(denoiserOnly)}% " +
                            "resamplers=${percent(resamplersOnly)}%",
                    )
                }
            }
        }
    }

    private fun percent(milliseconds: Double) = "%.2f".format(milliseconds / (seconds * 1_000) * 100)

    private fun time(block: () -> Unit): Double {
        val start = SystemClock.elapsedRealtimeNanos()
        block()
        return (SystemClock.elapsedRealtimeNanos() - start) / 1e6
    }

    private fun process(reducer: NoiseReducer, input: FloatArray, rate: Int, channels: Int) {
        val chunkFrames = rate / 50
        val buffer = FloatArray(chunkFrames * channels)
        var frame = 0
        while (frame + chunkFrames <= input.size / channels) {
            input.copyInto(buffer, 0, frame * channels, (frame + chunkFrames) * channels)
            reducer.process(buffer, chunkFrames)
            frame += chunkFrames
        }
        reducer.close()
    }

    private fun denoiseOnly(framesAtRate: Int, channels: Int) {
        val denoisers = List(channels) { RnnNoise() }
        val frame = FloatArray(RnnNoise.FRAME_SIZE) { it * 1e-4f }
        val out = FloatArray(RnnNoise.FRAME_SIZE)
        repeat((framesAtRate.toLong() * 48_000 / 44_100 / RnnNoise.FRAME_SIZE).toInt()) { denoisers.forEach { it.process(frame, out) } }
        denoisers.forEach { it.close() }
    }

    private fun resampleOnly(input: FloatArray, rate: Int, channels: Int) {
        val chunkFrames = rate / 50
        repeat(channels) {
            val up = Resampler(rate, 48_000, 1, NoiseReducer.SPEECH_PASSBAND)
            val down = Resampler(48_000, rate, 1, NoiseReducer.SPEECH_PASSBAND)
            val mono = FloatArray(chunkFrames)
            var frame = 0
            while (frame + chunkFrames <= input.size / channels) {
                for (index in 0 until chunkFrames) mono[index] = input[(frame + index) * channels]
                val produced = up.process(mono, 0, chunkFrames)
                down.process(up.output, 0, produced)
                frame += chunkFrames
            }
        }
    }

    private class RealDenoiser : FrameDenoiser {
        private val rnnNoise = RnnNoise()
        override val frameSize = RnnNoise.FRAME_SIZE

        override fun process(input: FloatArray, output: FloatArray) {
            rnnNoise.process(input, output)
        }

        override fun close() = rnnNoise.close()
    }

    private companion object {
        const val TAG = "NoiseReductionCost"
    }
}
