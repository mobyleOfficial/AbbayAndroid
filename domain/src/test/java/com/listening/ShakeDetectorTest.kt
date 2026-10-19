package com.listening

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthetic accelerometer traces at the rate of `SENSOR_DELAY_UI` (about 60 ms per sample), phone lying on its back
 * (gravity on z). Each trace counts the shakes a detector reports.
 */
class ShakeDetectorTest {
    @Test
    fun phoneLyingStillNeverShakes() {
        val trace = trace(seconds = 30.0) { _, noise -> Triple(noise(), noise(), GRAVITY + noise()) }
        assertEquals(0, shakes(trace, ShakeSensitivity.HIGH))
    }

    @Test
    fun walkingWithThePhoneIsNotAShake() {
        val trace = trace(seconds = 30.0) { time, noise ->
            val step = 4f * sin(2 * PI * STEP_HZ * time).toFloat()
            Triple(noise(), step * 0.5f + noise(), GRAVITY + step + noise())
        }
        assertEquals(0, shakes(trace, ShakeSensitivity.HIGH))
    }

    @Test
    fun aSingleJoltIsNotAShake() {
        val trace = trace(seconds = 5.0) { time, noise ->
            val jolt = if (time in 2.0..2.1) 35f else 0f
            Triple(jolt + noise(), noise(), GRAVITY + noise())
        }
        assertEquals(0, shakes(trace, ShakeSensitivity.HIGH))
    }

    @Test
    fun aFirmShakeIsDetectedOnceAtEverySensitivity() {
        val trace = shakeTrace(amplitude = 35f)
        assertEquals(1, shakes(trace, ShakeSensitivity.LOW))
        assertEquals(1, shakes(trace, ShakeSensitivity.MEDIUM))
        assertEquals(1, shakes(trace, ShakeSensitivity.HIGH))
    }

    @Test
    fun aGentleShakeNeedsAHigherSensitivity() {
        val trace = shakeTrace(amplitude = 15f)
        assertEquals(0, shakes(trace, ShakeSensitivity.LOW))
        assertEquals(1, shakes(trace, ShakeSensitivity.HIGH))
    }

    @Test
    fun twoSeparateShakesCountTwice() {
        val trace = trace(seconds = 8.0) { time, noise ->
            val isShaking = time in 1.0..2.0 || time in 5.0..6.0
            val shake = if (isShaking) 35f * sin(2 * PI * SHAKE_HZ * time).toFloat() else 0f
            Triple(shake + noise(), noise(), GRAVITY + noise())
        }
        assertEquals(2, shakes(trace, ShakeSensitivity.MEDIUM))
    }

    private fun shakeTrace(amplitude: Float) = trace(seconds = 5.0) { time, noise ->
        val shake = if (time in 2.0..3.2) amplitude * sin(2 * PI * SHAKE_HZ * time).toFloat() else 0f
        Triple(shake + noise(), shake * 0.3f + noise(), GRAVITY + noise())
    }

    private fun shakes(trace: List<Sample>, sensitivity: ShakeSensitivity): Int {
        val detector = ShakeDetector(sensitivity.thresholdMetersPerSecondSquared)
        return trace.count { detector.onSample(it.timestampNs, it.x, it.y, it.z) }
    }

    private fun trace(seconds: Double, signal: (time: Double, noise: () -> Float) -> Triple<Float, Float, Float>): List<Sample> {
        val random = Random(SEED)
        val noise = { (random.nextFloat() - 0.5f) * NOISE }
        return (0 until (seconds * 1_000 / SAMPLE_PERIOD_MS).toInt()).map { index ->
            val time = index * SAMPLE_PERIOD_MS / 1_000.0
            val (x, y, z) = signal(time, noise)
            Sample(index * SAMPLE_PERIOD_MS * 1_000_000L, x, y, z)
        }
    }

    private data class Sample(val timestampNs: Long, val x: Float, val y: Float, val z: Float)

    private companion object {
        const val GRAVITY = 9.81f
        const val SAMPLE_PERIOD_MS = 60L
        const val NOISE = 0.6f
        const val STEP_HZ = 1.8
        const val SHAKE_HZ = 3.5
        const val SEED = 7
    }
}
