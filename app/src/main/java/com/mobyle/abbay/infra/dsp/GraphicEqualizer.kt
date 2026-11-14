package com.mobyle.abbay.infra.dsp

import kotlin.math.abs
import kotlin.math.max

class GraphicEqualizer(private val sampleRate: Int, private val channelCount: Int) {
    private val bands = CENTERS_HZ.filter { it < sampleRate / 2.0 * NYQUIST_MARGIN }
    private val filters = Array(bands.size) { PeakingBiquad(channelCount) }
    private val bandIndices = IntArray(bands.size) { CENTERS_HZ.indexOf(bands[it]) }

    private var activeFilters = arrayOf<PeakingBiquad>()
    private val currentGains = FloatArray(BAND_COUNT)
    private val targetGains = FloatArray(BAND_COUNT)
    private val rampFrames = LinearRamp.framesFor(sampleRate)
    private val preamp = LinearRamp(1f, rampFrames)
    private var framesUntilUpdate = 0
    private var stepsLeft = 0

    init {
        updateCoefficients()
    }

    val isFlat: Boolean get() = stepsLeft == 0 && targetGains.all { it == 0f } && currentGains.all { it == 0f }

    fun setGains(gainsDb: FloatArray, immediately: Boolean = false) {
        for (band in 0 until BAND_COUNT) targetGains[band] = gainsDb.getOrElse(band) { 0f }.coerceIn(-MAXIMUM_DB, MAXIMUM_DB)
        val preampGain = Decibels.toGain(-max(0f, targetGains.max()))
        if (immediately) {
            targetGains.copyInto(currentGains)
            stepsLeft = 0
            preamp.snapTo(preampGain)
            updateCoefficients()
        } else {
            stepsLeft = max(1, rampFrames / UPDATE_FRAMES)
            preamp.moveTo(preampGain)
        }
    }

    fun reset() = filters.forEach(PeakingBiquad::reset)

    fun process(samples: FloatArray, frameCount: Int) {
        for (frame in 0 until frameCount) {
            if (stepsLeft > 0 && framesUntilUpdate-- <= 0) stepGains()
            val gain = preamp.next()
            val base = frame * channelCount
            val active = activeFilters
            for (channel in 0 until channelCount) {
                var value = samples[base + channel] * gain
                for (index in active.indices) value = active[index].process(value, channel)
                samples[base + channel] = value
            }
        }
    }

    private fun stepGains() {
        framesUntilUpdate = UPDATE_FRAMES - 1
        for (band in 0 until BAND_COUNT) {
            val difference = targetGains[band] - currentGains[band]
            currentGains[band] += if (stepsLeft == 1) difference else difference / stepsLeft
            if (abs(targetGains[band] - currentGains[band]) < EPSILON_DB) currentGains[band] = targetGains[band]
        }
        stepsLeft--
        updateCoefficients()
    }

    private fun updateCoefficients() {
        val active = ArrayList<PeakingBiquad>(filters.size)
        filters.forEachIndexed { index, filter ->
            val band = bandIndices[index]
            filter.setPeaking(sampleRate, bands[index], Q, currentGains[band].toDouble())
            if (currentGains[band] != 0f || targetGains[band] != 0f) {
                active += filter
            } else {
                filter.reset()
            }
        }
        activeFilters = active.toTypedArray()
    }

    companion object {
        const val BAND_COUNT = 10
        const val MAXIMUM_DB = 12f
        val CENTERS_HZ = listOf(31.5, 63.0, 125.0, 250.0, 500.0, 1_000.0, 2_000.0, 4_000.0, 8_000.0, 16_000.0)
        private const val Q = 1.41
        private const val UPDATE_FRAMES = 32
        private const val EPSILON_DB = 0.001f

        private const val NYQUIST_MARGIN = 0.95
    }
}
