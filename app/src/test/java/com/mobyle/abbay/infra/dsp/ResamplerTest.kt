package com.mobyle.abbay.infra.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class ResamplerTest {
    @Test
    fun sineFrom44100To48000HasNoSpuriousAbove80Db() {
        val residual = spuriousDb(1_000.0, inputRate = 44_100, outputRate = 48_000)
        assertTrue("spurious at $residual dB", residual < -80)
    }

    @Test
    fun otherCommonRatesStayClean() {
        assertTrue(spuriousDb(1_000.0, 48_000, 44_100) < -80)
        assertTrue(spuriousDb(3_000.0, 22_050, 48_000) < -80)
        assertTrue(spuriousDb(440.0, 16_000, 48_000) < -80)
    }

    @Test
    fun toneAboveTheNewNyquistIsRemoved() {
        val input = Signals.sine(23_000.0, 1.0, 0.5f, rate = 48_000)
        val output = run(Resampler(48_000, 44_100, 1), input, chunk = 1_000)
        val level = Signals.rms(output, from = 2_000, to = output.size - 2_000) / (0.5 / sqrt(2.0))
        assertTrue("alias at ${20 * log10(level)} dB", 20 * log10(level) < -80)
    }

    @Test
    fun lengthFollowsTheRatioAndChannelsStayApart() {
        val stereo = FloatArray(44_100 * 2) { index -> if (index % 2 == 0) 0.5f else -0.25f }
        val output = run(Resampler(44_100, 48_000, 2), stereo, chunk = 512)
        assertEquals(48_000.0, output.size / 2.0, 2.0)
        assertEquals(0.5, output[48_000].toDouble(), 1e-3)
        assertEquals(-0.25, output[48_001].toDouble(), 1e-3)
    }

    /** Level of everything but the expected tone, relative to the tone, after the filter settled. */
    private fun spuriousDb(frequencyHz: Double, inputRate: Int, outputRate: Int): Double {
        val output = run(Resampler(inputRate, outputRate, 1), Signals.sine(frequencyHz, 1.0, 0.5f, rate = inputRate), chunk = 777)
        val from = outputRate / 10
        val to = output.size - outputRate / 10
        // Least-squares fit of a sine and a cosine at the tone's frequency; what is left is spurious.
        var sinSin = 0.0
        var cosCos = 0.0
        var sinCos = 0.0
        var outSin = 0.0
        var outCos = 0.0
        for (n in from until to) {
            val angle = 2 * PI * frequencyHz * n / outputRate
            val s = sin(angle)
            val c = cos(angle)
            sinSin += s * s
            cosCos += c * c
            sinCos += s * c
            outSin += output[n] * s
            outCos += output[n] * c
        }
        val determinant = sinSin * cosCos - sinCos * sinCos
        val a = (outSin * cosCos - outCos * sinCos) / determinant
        val b = (outCos * sinSin - outSin * sinCos) / determinant
        var residual = 0.0
        var signal = 0.0
        for (n in from until to) {
            val angle = 2 * PI * frequencyHz * n / outputRate
            val fitted = a * sin(angle) + b * cos(angle)
            residual += (output[n] - fitted) * (output[n] - fitted)
            signal += fitted * fitted
        }
        return 10 * log10(residual / signal)
    }

    private fun run(resampler: Resampler, input: FloatArray, chunk: Int): FloatArray {
        val parts = mutableListOf<FloatArray>()
        var position = 0
        while (position < input.size) {
            val end = minOf(input.size, position + chunk * 2)
            parts += resampler.process(input.copyOfRange(position, end))
            position = end
        }
        parts += resampler.flush()
        return parts.fold(FloatArray(0)) { all, part -> all + part }
    }
}
