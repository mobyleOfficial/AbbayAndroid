package com.mobyle.abbay.infra.dsp.nativelib

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt
import kotlin.random.Random

/** The native library loads on the device and lowers steady noise. */
@RunWith(AndroidJUnit4::class)
class RnnNoiseTest {
    @Test
    fun loadsAndAttenuatesWhiteNoise() {
        assertTrue(RnnNoise.isAvailable)
        assertEquals(RnnNoise.FRAME_SIZE, RnnNoise.nativeFrameSizeOrZero())
        val random = Random(7)
        val input = FloatArray(RnnNoise.FRAME_SIZE)
        val output = FloatArray(RnnNoise.FRAME_SIZE)
        var inputEnergy = 0.0
        var outputEnergy = 0.0
        RnnNoise().use { denoiser ->
            repeat(300) { frame ->
                for (index in input.indices) input[index] = (random.nextFloat() * 2 - 1) * 0.1f
                denoiser.process(input, output)
                // After 1 s the noise estimate has settled.
                if (frame >= 100) {
                    inputEnergy += input.sumOf { it * it.toDouble() }
                    outputEnergy += output.sumOf { it * it.toDouble() }
                }
            }
        }
        val attenuationDb = 10 * kotlin.math.log10(outputEnergy / inputEnergy)
        // RNNoise v0.1 lowers full-band white noise by about 3 dB (measured on the emulator); speech noise far more.
        assertTrue("noise changed by $attenuationDb dB", attenuationDb < -2)
        assertTrue(sqrt(outputEnergy).isFinite())
    }
}
