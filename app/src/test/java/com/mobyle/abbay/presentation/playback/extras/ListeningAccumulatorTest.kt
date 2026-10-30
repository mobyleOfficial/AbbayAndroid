package com.mobyle.abbay.presentation.playback.extras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ListeningAccumulatorTest {
    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun bookTimeFollowsTheSpeedAndTheTrimmedSilence() {
        val accumulator = ListeningAccumulator()
        accumulator.play("a", nowMs = 0, speed = 1f)
        accumulator.setSpeed(nowMs = 60_000, speed = 2f)
        accumulator.addSilence("a", 5_000)
        accumulator.addSilence("other", 9_000)

        val day = accumulator.take(nowMs = 120_000, date = today)!!

        assertEquals(120_000L, day.wallTimeMs)
        assertEquals(60_000L + 120_000L + 5_000L, day.bookTimeMs)
        assertEquals(60_000L, day.savedBySpeedMs)
        assertEquals(5_000L, day.savedBySilenceMs)
    }

    @Test
    fun pausesAreNotCountedAndTakeStartsOver() {
        val accumulator = ListeningAccumulator()
        accumulator.play("a", nowMs = 0, speed = 1f)
        accumulator.pause(nowMs = 10_000)
        accumulator.play("a", nowMs = 50_000, speed = 1f)

        assertEquals(15_000L, accumulator.take(nowMs = 55_000, date = today)!!.wallTimeMs)
        // Still playing: the next take measures from the previous one.
        assertEquals(5_000L, accumulator.take(nowMs = 60_000, date = today)!!.wallTimeMs)
        accumulator.pause(nowMs = 60_000)
        assertNull(accumulator.take(nowMs = 90_000, date = today))
    }

    @Test
    fun slowSpeedsSaveNothingAndAnotherBookStartsClean() {
        val accumulator = ListeningAccumulator()
        accumulator.play("a", nowMs = 0, speed = 0.5f)
        val slow = accumulator.take(nowMs = 10_000, date = today)!!
        assertEquals(5_000L, slow.bookTimeMs)
        assertEquals(0L, slow.savedBySpeedMs)

        accumulator.play("b", nowMs = 20_000, speed = 1f)
        val other = accumulator.take(nowMs = 30_000, date = today)!!
        assertEquals("b", other.bookId)
        assertEquals(10_000L, other.wallTimeMs)
    }
}
