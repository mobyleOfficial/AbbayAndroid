package com.listening

import kotlin.math.sqrt

class ShakeDetector(private val thresholdMetersPerSecondSquared: Float) {
    private val gravity = FloatArray(AXES)
    private var hasGravity = false
    private var isAboveThreshold = false
    private val peakTimesNs = ArrayDeque<Long>()
    private var lastShakeNs: Long? = null

    @Suppress("ReturnCount")
    fun onSample(timestampNs: Long, x: Float, y: Float, z: Float): Boolean {
        val acceleration = floatArrayOf(x, y, z)
        if (!hasGravity) {
            acceleration.copyInto(gravity)
            hasGravity = true
            return false
        }
        for (axis in 0 until AXES) {
            gravity[axis] = GRAVITY_FILTER * gravity[axis] + (1 - GRAVITY_FILTER) * acceleration[axis]
        }
        val linearX = acceleration[0] - gravity[0]
        val linearY = acceleration[1] - gravity[1]
        val linearZ = acceleration[2] - gravity[2]
        val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        val wasAboveThreshold = isAboveThreshold
        isAboveThreshold = magnitude >= thresholdMetersPerSecondSquared
        if (!isAboveThreshold || wasAboveThreshold) return false
        if (lastShakeNs?.let { timestampNs - it < COOLDOWN_NS } == true) return false

        peakTimesNs.addLast(timestampNs)
        while (timestampNs - peakTimesNs.first() > WINDOW_NS) peakTimesNs.removeFirst()
        if (peakTimesNs.size < PEAKS_PER_SHAKE) return false

        peakTimesNs.clear()
        lastShakeNs = timestampNs
        return true
    }

    fun reset() {
        hasGravity = false
        isAboveThreshold = false
        peakTimesNs.clear()
        lastShakeNs = null
    }

    private companion object {
        const val AXES = 3
        const val GRAVITY_FILTER = 0.8f
        const val PEAKS_PER_SHAKE = 3
        const val WINDOW_NS = 1_000_000_000L

        const val COOLDOWN_NS = 1_500_000_000L
    }
}
