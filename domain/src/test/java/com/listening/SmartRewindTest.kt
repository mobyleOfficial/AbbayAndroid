package com.listening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRewindTest {
    @Test
    fun standardMatchesTheSpecExamples() {
        assertNear(3_600L, rewind(5))
        assertNear(9_800L, rewind(60))
        assertNear(18_400L, rewind(600))
        assertNear(25_600L, rewind(3_600))
        assertEquals(30_000L, rewind(8 * 3_600))
    }

    @Test
    fun levelsCapAtTheirMaximum() {
        val week = 7 * 24 * 3_600 * 1_000L
        assertEquals(15_000L, SmartRewind.rewindMs(week, SmartRewindLevel.LIGHT))
        assertEquals(30_000L, SmartRewind.rewindMs(week, SmartRewindLevel.STANDARD))
        assertEquals(60_000L, SmartRewind.rewindMs(week, SmartRewindLevel.GENEROUS))
    }

    @Test
    fun offAndShortPausesDoNotRewind() {
        assertEquals(0L, SmartRewind.rewindMs(3_600_000L, SmartRewindLevel.OFF))
        assertEquals(0L, SmartRewind.rewindMs(999L, SmartRewindLevel.GENEROUS))
        assertNear(2_380L, SmartRewind.rewindMs(1_000L, SmartRewindLevel.STANDARD))
    }

    @Test
    fun resumeRewindsFromThePausedPosition() {
        val position = SmartRewind.resumePositionMs(100_000L, 100_000L, 60_000L, SmartRewindLevel.STANDARD)
        assertNear(90_200L, position)
    }

    @Test
    fun aMoveSinceThePauseCancelsTheRewind() {
        val afterSeek = SmartRewind.resumePositionMs(400_000L, 100_000L, 3_600_000L, SmartRewindLevel.STANDARD)
        assertEquals(400_000L, afterSeek)
    }

    @Test
    fun sleepTimerExtraIsAddedAndTheStartOfTheBookIsTheLimit() {
        val withFade = SmartRewind.resumePositionMs(100_000L, 100_000L, 60_000L, SmartRewindLevel.STANDARD, extraRewindMs = 30_000L)
        assertNear(60_200L, withFade)
        assertEquals(0L, SmartRewind.resumePositionMs(4_000L, 4_000L, 3_600_000L, SmartRewindLevel.GENEROUS))
    }

    private fun rewind(pauseSeconds: Int) = SmartRewind.rewindMs(pauseSeconds * 1_000L, SmartRewindLevel.STANDARD)

    private fun assertNear(expected: Long, actual: Long) =
        assertTrue("expected $expected ± $TOLERANCE_MS, was $actual", kotlin.math.abs(expected - actual) <= TOLERANCE_MS)

    private companion object {
        const val TOLERANCE_MS = 50L
    }
}
