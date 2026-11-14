package com.listening

import kotlin.math.abs

class MotionGestureDetector {
    enum class Gesture { FACE_DOWN, FACE_UP, FLIP }

    private var faceDownSinceNs: Long? = null
    private var faceUpSinceNs: Long? = null
    private var isFaceDown = false
    private var lastFlipNs: Long? = null
    private var isRotating = false

    fun onAcceleration(timestampNs: Long, z: Float): Gesture? = if (isFaceDown) faceUp(timestampNs, z) else faceDown(timestampNs, z)

    fun resetFace() {
        isFaceDown = false
        faceDownSinceNs = null
        faceUpSinceNs = null
    }

    fun onRotation(timestampNs: Long, yRadiansPerSecond: Float): Gesture? {
        val wasRotating = isRotating
        isRotating = abs(yRadiansPerSecond) > FLIP_RADIANS_PER_SECOND
        val isCoolingDown = lastFlipNs?.let { timestampNs - it < FLIP_COOLDOWN_NS } == true
        if (!isRotating || wasRotating || isCoolingDown) return null
        lastFlipNs = timestampNs
        return Gesture.FLIP
    }

    private fun faceDown(timestampNs: Long, z: Float): Gesture? {
        if (z >= FACE_DOWN_Z) {
            faceDownSinceNs = null
            return null
        }
        val since = faceDownSinceNs ?: timestampNs.also { faceDownSinceNs = it }
        if (timestampNs - since < FACE_DOWN_HOLD_NS) return null
        isFaceDown = true
        faceDownSinceNs = null
        return Gesture.FACE_DOWN
    }

    private fun faceUp(timestampNs: Long, z: Float): Gesture? {
        if (z <= FACE_UP_Z) {
            faceUpSinceNs = null
            return null
        }
        val since = faceUpSinceNs ?: timestampNs.also { faceUpSinceNs = it }
        if (timestampNs - since < FACE_UP_HOLD_NS) return null
        isFaceDown = false
        faceUpSinceNs = null
        return Gesture.FACE_UP
    }

    companion object {
        const val FACE_DOWN_Z = -8f
        const val FACE_UP_Z = 0f
        const val FLIP_RADIANS_PER_SECOND = 4f
        const val FACE_DOWN_HOLD_NS = 1_000_000_000L
        const val FACE_UP_HOLD_NS = 500_000_000L
        const val FLIP_COOLDOWN_NS = 1_000_000_000L

        const val RESUME_WINDOW_MS = 10 * 60_000L
    }
}
