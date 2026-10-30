package com.listening

import com.listening.MotionGestureDetector.Gesture
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionGestureDetectorTest {
    private val millisecond = 1_000_000L

    /** Feeds [z] every 20 ms for [durationMs], starting at [startMs]; returns the gestures with their times. */
    private fun MotionGestureDetector.hold(startMs: Long, durationMs: Long, z: Float): List<Pair<Long, Gesture>> =
        (startMs until startMs + durationMs step 20).mapNotNull { time -> onAcceleration(time * millisecond, z)?.let { time to it } }

    @Test
    fun faceDownNeedsOneSecondBelowMinusEight() {
        val detector = MotionGestureDetector()
        assertEquals(emptyList<Any>(), detector.hold(0, 600, -9.5f))
        // A tilt back resets the hold.
        assertEquals(emptyList<Any>(), detector.hold(600, 100, -5f))
        assertEquals(listOf(1_700L to Gesture.FACE_DOWN), detector.hold(700, 2_000, -9.6f))
    }

    @Test
    fun faceUpAfterFaceDownNeedsHalfASecond() {
        val detector = MotionGestureDetector()
        detector.hold(0, 1_100, -9.8f)
        // Picked up for 0.3 s, then put back down: not enough.
        assertEquals(emptyList<Any>(), detector.hold(1_100, 300, 4f))
        assertEquals(emptyList<Any>(), detector.hold(1_400, 100, -2f))
        assertEquals(listOf(2_000L to Gesture.FACE_UP), detector.hold(1_500, 1_000, 9.8f))
        // Lying face up on a table is not a gesture.
        assertEquals(emptyList<Any>(), MotionGestureDetector().hold(0, 3_000, 9.8f))
    }

    @Test
    fun aResetAllowsTheNextFaceDown() {
        val detector = MotionGestureDetector()
        detector.hold(0, 1_100, -9.8f)
        detector.resetFace()
        assertEquals(listOf(2_100L to Gesture.FACE_DOWN), detector.hold(1_100, 1_100, -9.8f))
    }

    @Test
    fun flipIsAQuickRotationAroundTheLongAxisAtMostOnceASecond() {
        val detector = MotionGestureDetector()
        // A held rotation counts once; after it stops, a new one within 1 s is ignored, a later one counts.
        val trace = listOf(
            0L to 1f,
            20L to 3.9f,
            40L to 6f,
            60L to -7f,
            600L to 0f,
            700L to 5f,
            800L to 0f,
            1_500L to -4.5f,
            1_600L to 6f,
        )
        val flips = trace.mapNotNull { (time, speed) -> detector.onRotation(time * millisecond, speed)?.let { time } }
        assertEquals(listOf(40L, 1_500L), flips)
    }
}
