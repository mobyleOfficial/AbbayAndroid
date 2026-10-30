package com.mobyle.abbay.infra.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Peaking EQ biquad from the RBJ *Audio EQ Cookbook*, Direct Form II transposed, one state per channel. Coefficients
 * are computed in double precision.
 */
class PeakingBiquad(private val channelCount: Int) {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private val state1 = DoubleArray(channelCount)
    private val state2 = DoubleArray(channelCount)

    fun setPeaking(sampleRate: Int, centerHz: Double, q: Double, gainDb: Double) {
        val amplitude = 10.0.pow(gainDb / 40.0)
        val omega = 2.0 * PI * centerHz / sampleRate
        val alpha = sin(omega) / (2.0 * q)
        val cosine = cos(omega)
        val a0 = 1.0 + alpha / amplitude
        b0 = (1.0 + alpha * amplitude) / a0
        b1 = -2.0 * cosine / a0
        b2 = (1.0 - alpha * amplitude) / a0
        a1 = -2.0 * cosine / a0
        a2 = (1.0 - alpha / amplitude) / a0
    }

    fun reset() {
        state1.fill(0.0)
        state2.fill(0.0)
    }

    fun process(sample: Float, channel: Int): Float {
        val input = sample.toDouble()
        val output = b0 * input + state1[channel]
        state1[channel] = b1 * input - a1 * output + state2[channel]
        state2[channel] = b2 * input - a2 * output
        return output.toFloat()
    }
}
