package com.mobyle.abbay.infra.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LoudnessAndLimiterTest {
    private val rate = Signals.RATE
    private val ceiling = Decibels.toGain(PeakLimiter.CEILING_DB)

    @Test
    fun mediumBoostRaisesQuietSpeechBySixDecibelsWithoutPassingTheCeiling() {
        val quiet = Signals.bursts(burstSeconds = 0.8, silenceSeconds = 0.2, count = 6, level = 1f)
        val scale = Decibels.toGain(-30f) / Signals.rms(quiet).toFloat()
        val input = FloatArray(quiet.size) { quiet[it] * scale }
        val output = input.copyOf()

        val compressor = LoudnessCompressor(1, rate).apply { set(LoudnessCompressor.Settings(-28f, 3f, 8f), immediately = true) }
        val limiter = PeakLimiter(1, rate)
        compressor.process(output, output.size)
        limiter.process(output, output.size)

        assertTrue(Signals.decibels(Signals.rms(output) / Signals.rms(input)) >= 6.0)
        assertTrue(Signals.peak(output) <= ceiling + 1e-6f)
    }

    @Test
    fun theLimiterHoldsAFullScaleSinePlusSixDecibelsUnderMinusOne() {
        val input = Signals.sine(440.0, 1.0, Decibels.toGain(6f), channels = 2)
        val limiter = PeakLimiter(2, rate)
        limiter.process(input, input.size / 2)
        assertTrue(Signals.peak(input) <= ceiling + 1e-6f)
    }

    @Test
    fun belowTheCeilingTheLimiterIsTransparent() {
        val input = Signals.sine(1_000.0, 1.0, Decibels.toGain(-20f))
        val output = input.copyOf()
        PeakLimiter(1, rate).process(output, output.size)
        assertTrue(totalHarmonicDistortion(output.copyOfRange(rate / 2, output.size), 1_000.0) < 0.001)
    }

    @Test
    fun monoMakesBothChannelsTheMeanOfALeftOnlySignal() {
        val left = Signals.sine(500.0, 0.5, 0.5f)
        val stereo = FloatArray(left.size * 2) { if (it % 2 == 0) left[it / 2] else 0f }
        ChannelMixer(2, rate).apply { set(mono = true, balance = 0f, immediately = true) }.process(stereo, left.size)

        val leftLevel = Signals.rms(stereo, channels = 2, channel = 0)
        val rightLevel = Signals.rms(stereo, channels = 2, channel = 1)
        assertEquals(leftLevel, rightLevel, 1e-6)
        assertEquals(-6.02, Signals.decibels(leftLevel / Signals.rms(left)), 0.05)
    }

    @Test
    fun balanceFollowsTheSpecGains() {
        val stereo = FloatArray(2 * rate) { 0.5f }
        ChannelMixer(2, rate).apply { set(mono = false, balance = 0.5f, immediately = true) }.process(stereo, rate)
        assertEquals(0.25f, stereo[0], 1e-6f)
        assertEquals(0.5f, stereo[1], 1e-6f)
    }

    @Test
    fun parameterChangesMidBufferDoNotJump() {
        val input = Signals.sine(300.0, 1.0, 0.5f, channels = 2)
        val output = input.copyOf()
        val frames = input.size / 2
        val mixer = ChannelMixer(2, rate)
        val compressor = LoudnessCompressor(2, rate)
        val half = frames / 2
        val first = output.copyOfRange(0, half * 2)
        mixer.process(first, half)
        compressor.process(first, half)
        mixer.set(mono = true, balance = -1f)
        compressor.set(LoudnessCompressor.Settings(-32f, 4f, 12f))
        val second = output.copyOfRange(half * 2, output.size)
        mixer.process(second, frames - half)
        compressor.process(second, frames - half)
        first.copyInto(output)
        second.copyInto(output, half * 2)

        for (channel in 0..1) {
            assertTrue(Signals.maximumStep(output, 2, channel) <= 2 * Signals.maximumStep(input, 2, channel) * Decibels.toGain(12f))
        }
        val around = output.copyOfRange((half - 2_000) * 2, (half + 2_000) * 2)
        val inputAround = input.copyOfRange((half - 2_000) * 2, (half + 2_000) * 2)
        assertTrue(Signals.maximumStep(around, 2, 0) <= 2 * Signals.maximumStep(inputAround, 2, 0))
    }

    private fun totalHarmonicDistortion(samples: FloatArray, fundamentalHz: Double): Double {
        fun magnitude(frequency: Double): Double {
            var real = 0.0
            var imaginary = 0.0
            samples.forEachIndexed { index, sample ->
                val angle = 2 * PI * frequency * index / rate
                real += sample * cos(angle)
                imaginary -= sample * sin(angle)
            }
            return sqrt(real * real + imaginary * imaginary)
        }
        val fundamental = magnitude(fundamentalHz)
        val harmonics = (2..10).sumOf { harmonic -> magnitude(fundamentalHz * harmonic).let { it * it } }
        return sqrt(harmonics) / fundamental
    }
}
