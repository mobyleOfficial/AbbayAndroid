package com.mobyle.abbay.infra.dsp

import kotlin.math.min

/**
 * Mono and balance. Mono: every channel is the mean of the input channels.
 * Balance −1…+1: left gain `min(1, 1 − balance)`, right gain `min(1, 1 + balance)`. Changes ramp over 20 ms.
 * Interleaved float samples, processed in place.
 */
class ChannelMixer(private val channelCount: Int, sampleRate: Int) {
    private val rampFrames = LinearRamp.framesFor(sampleRate)
    private val monoAmount = LinearRamp(0f, rampFrames)
    private val leftGain = LinearRamp(1f, rampFrames)
    private val rightGain = LinearRamp(1f, rampFrames)

    fun set(mono: Boolean, balance: Float, immediately: Boolean = false) {
        val clamped = balance.coerceIn(-1f, 1f)
        val left = min(1f, 1f - clamped)
        val right = min(1f, 1f + clamped)
        val monoTarget = if (mono) 1f else 0f
        if (immediately) {
            monoAmount.snapTo(monoTarget)
            leftGain.snapTo(left)
            rightGain.snapTo(right)
        } else {
            monoAmount.moveTo(monoTarget)
            leftGain.moveTo(left)
            rightGain.moveTo(right)
        }
    }

    /** True when the output equals the input. */
    val isIdentity: Boolean
        get() = !monoAmount.isRamping &&
            !leftGain.isRamping &&
            !rightGain.isRamping &&
            monoAmount.target == 0f &&
            leftGain.target == 1f &&
            rightGain.target == 1f

    fun process(samples: FloatArray, frameCount: Int) {
        if (channelCount < 2) return
        for (frame in 0 until frameCount) {
            val base = frame * channelCount
            var sum = 0f
            for (channel in 0 until channelCount) sum += samples[base + channel]
            val mean = sum / channelCount
            val mono = monoAmount.next()
            val left = leftGain.next()
            val right = rightGain.next()
            for (channel in 0 until channelCount) {
                val mixed = samples[base + channel] * (1f - mono) + mean * mono
                val gain = when (channel) {
                    0 -> left
                    1 -> right
                    else -> 1f
                }
                samples[base + channel] = mixed * gain
            }
        }
    }
}
