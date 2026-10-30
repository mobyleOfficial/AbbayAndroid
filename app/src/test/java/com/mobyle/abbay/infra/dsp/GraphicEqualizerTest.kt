package com.mobyle.abbay.infra.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GraphicEqualizerTest {
    @Test
    fun eachBandGivesSixDecibelsAtItsCenterAndLittleTwoOctavesAway() {
        val centers = GraphicEqualizer.CENTERS_HZ
        centers.forEachIndexed { band, center ->
            val gains = FloatArray(GraphicEqualizer.BAND_COUNT).also { it[band] = 6f }
            val preampDb = -6.0
            assertEquals("band $center Hz", 6.0, gainDb(gains, center) - preampDb, 0.5)
            listOf(center / 4, center * 4).filter { it in 20.0..20_000.0 }.forEach { far ->
                assertTrue("band $center Hz at $far Hz", abs(gainDb(gains, far) - preampDb) < 1.0)
            }
        }
    }

    @Test
    fun flatIsBypassed() {
        val equalizer = GraphicEqualizer(Signals.RATE, 2)
        assertTrue(equalizer.isFlat)
        equalizer.setGains(FloatArray(10) { if (it == 3) 4f else 0f })
        assertTrue(!equalizer.isFlat)
        equalizer.setGains(FloatArray(10), immediately = true)
        assertTrue(equalizer.isFlat)
    }

    @Test
    fun aGainChangeInTheMiddleOfABufferDoesNotJump() {
        val input = Signals.sine(1_000.0, 1.0, 0.5f)
        val output = input.copyOf()
        val equalizer = GraphicEqualizer(Signals.RATE, 1)
        val half = input.size / 2
        equalizer.process(output, half)
        equalizer.setGains(FloatArray(10) { if (it == 5) 12f else -12f })
        val rest = output.copyOfRange(half, output.size)
        equalizer.process(rest, rest.size)
        rest.copyInto(output, half)
        assertTrue(Signals.maximumStep(output) <= 2 * Signals.maximumStep(input))
    }

    private fun gainDb(gains: FloatArray, frequency: Double): Double {
        val equalizer = GraphicEqualizer(Signals.RATE, 1)
        equalizer.setGains(gains, immediately = true)
        val input = Signals.sine(frequency, 2.0, 0.1f)
        val output = input.copyOf()
        equalizer.process(output, output.size)
        val settled = output.size / 2
        return Signals.decibels(Signals.rms(output, settled) / Signals.rms(input, settled))
    }
}
