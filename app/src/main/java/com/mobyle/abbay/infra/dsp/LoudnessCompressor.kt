package com.mobyle.abbay.infra.dsp

import kotlin.math.exp
import kotlin.math.max

/**
 * Feed-forward compressor of the loudness boost: RMS detector over 30 ms, attack
 * 15 ms, release 250 ms, 6 dB soft knee, channels linked, then make-up gain. A level change moves the make-up gain
 * over 20 ms; the gain computer follows with its own attack and release. Interleaved float samples, in place.
 */
class LoudnessCompressor(private val channelCount: Int, private val sampleRate: Int) {
    /** One row of the boost table; [OFF] leaves the signal unchanged. */
    data class Settings(val thresholdDb: Float, val ratio: Float, val makeUpDb: Float) {
        companion object {
            val OFF = Settings(0f, 1f, 0f)
        }
    }

    private var settings = Settings.OFF
    private val detectorCoefficient = coefficient(DETECTOR_MS)

    /** Attack and release per control step of [CONTROL_FRAMES] frames. */
    private val attackControl = Math.pow(coefficient(ATTACK_MS), CONTROL_FRAMES.toDouble())
    private val releaseControl = Math.pow(coefficient(RELEASE_MS), CONTROL_FRAMES.toDouble())
    private var meanSquare = 0.0
    private var gainReductionDb = 0.0
    private val makeUp = LinearRamp(1f, LinearRamp.framesFor(sampleRate))

    val isOff: Boolean get() = settings == Settings.OFF && !makeUp.isRamping && makeUp.current == 1f && gainReductionDb < OFF_REDUCTION_DB

    fun set(newSettings: Settings, immediately: Boolean = false) {
        settings = newSettings
        val gain = Decibels.toGain(newSettings.makeUpDb)
        if (immediately) makeUp.snapTo(gain) else makeUp.moveTo(gain)
    }

    fun reset() {
        meanSquare = 0.0
        gainReductionDb = 0.0
        framesUntilControl = 0
        controlGain = 1f
        controlStep = 0f
    }

    /**
     * The detector runs every frame; the gain computer (a logarithm and a power) every [CONTROL_FRAMES] frames, and the
     * linear gain is interpolated between control points, which keeps the cost low without steps in the gain.
     */
    fun process(samples: FloatArray, frameCount: Int) {
        for (frame in 0 until frameCount) {
            val base = frame * channelCount
            var power = 0.0
            for (channel in 0 until channelCount) {
                val value = samples[base + channel].toDouble()
                power += value * value
            }
            meanSquare = detectorCoefficient * meanSquare + (1 - detectorCoefficient) * (power / channelCount)

            if (framesUntilControl-- <= 0) updateControl()
            controlGain += controlStep
            val gain = controlGain * makeUp.next()
            for (channel in 0 until channelCount) samples[base + channel] *= gain
        }
    }

    private var framesUntilControl = 0
    private var controlGain = 1f
    private var controlStep = 0f

    /** Advances the gain computer by [CONTROL_FRAMES] frames and sets the ramp to the new gain. */
    private fun updateControl() {
        framesUntilControl = CONTROL_FRAMES - 1
        val levelDb = if (meanSquare <= SILENCE_POWER) SILENCE_DB else 10.0 * kotlin.math.log10(meanSquare)
        val targetReduction = reductionDb(levelDb)
        val coefficient = if (targetReduction > gainReductionDb) attackControl else releaseControl
        gainReductionDb = coefficient * gainReductionDb + (1 - coefficient) * targetReduction
        val target = Decibels.toGain(-gainReductionDb.toFloat())
        controlStep = (target - controlGain) / CONTROL_FRAMES
    }

    /** Soft-knee gain computer: how many dB to take away at [levelDb]. */
    private fun reductionDb(levelDb: Double): Double {
        val threshold = settings.thresholdDb.toDouble()
        val slope = 1.0 - 1.0 / settings.ratio
        val overshoot = levelDb - threshold
        return when {
            settings.ratio <= 1f -> 0.0
            2 * overshoot < -KNEE_DB -> 0.0
            2 * overshoot > KNEE_DB -> slope * overshoot
            else -> slope * (overshoot + KNEE_DB / 2).let { it * it } / (2 * KNEE_DB)
        }.let { max(0.0, it) }
    }

    private fun coefficient(milliseconds: Double): Double = exp(-1.0 / (milliseconds / 1_000.0 * sampleRate))

    private companion object {
        const val DETECTOR_MS = 30.0
        const val ATTACK_MS = 15.0
        const val RELEASE_MS = 250.0
        const val KNEE_DB = 6.0
        const val SILENCE_POWER = 1e-12
        const val SILENCE_DB = -120.0

        /** Below this reduction the gain is 1 to float precision, so the compressor can be skipped without a step. */
        const val OFF_REDUCTION_DB = 1e-4

        /** 16 frames: 0.33 ms at 48 kHz, far below the 15 ms attack. */
        const val CONTROL_FRAMES = 16
    }
}
