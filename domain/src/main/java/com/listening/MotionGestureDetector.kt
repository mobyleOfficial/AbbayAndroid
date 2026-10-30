package com.listening

import kotlin.math.abs

/**
 * Motion gestures, from raw sensor samples (timestamps in nanoseconds).
 * Face down: accelerometer z below −8 m/s² for 1 s. Face up again: z above 0 for 0.5 s after a face-down. Flip: an
 * angular speed above 4 rad/s around the long (y) axis, once per rotation and at most once a second. Pure logic,
 * tested with synthetic traces.
 */
class MotionGestureDetector {
    enum class Gesture { FACE_DOWN, FACE_UP, FLIP }

    private var faceDownSinceNs: Long? = null
    private var faceUpSinceNs: Long? = null
    private var isFaceDown = false
    private var lastFlipNs: Long? = null
    private var isRotating = false

    /** Accelerometer sample (m/s², gravity included); returns a face gesture when this sample completes one. */
    fun onAcceleration(timestampNs: Long, z: Float): Gesture? = if (isFaceDown) faceUp(timestampNs, z) else faceDown(timestampNs, z)

    /** Playback resumed some other way: the next face-down counts again. */
    fun resetFace() {
        isFaceDown = false
        faceDownSinceNs = null
        faceUpSinceNs = null
    }

    /** Gyroscope sample around the long axis (rad/s). */
    fun onRotation(timestampNs: Long, yRadiansPerSecond: Float): Gesture? {
        val wasRotating = isRotating
        isRotating = abs(yRadiansPerSecond) > FLIP_RADIANS_PER_SECOND
        // One flip per rotation: the speed must drop below the threshold before the next one counts.
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

        /** Face up within this time after a face-down pause resumes playback. */
        const val RESUME_WINDOW_MS = 10 * 60_000L
    }
}
