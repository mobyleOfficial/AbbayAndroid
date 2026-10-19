package com.listening

import com.player.SleepTimerMode
import com.player.SleepTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    private val timer = SleepTimer()

    @Test
    fun durationCountsListeningTimeAndStops() {
        timer.start(SleepTimerMode.Duration(5), fadeMs = 0L)
        assertEquals(running(SleepTimerMode.Duration(5), 300_000L, false), timer.state)

        repeat(299) { assertFalse(timer.advance(1_000L, null).shouldStop) }
        assertEquals(1_000L, (timer.state as SleepTimerState.Running).remainingMs)

        val last = timer.advance(1_000L, null)
        assertTrue(last.shouldStop)
        assertEquals(1f, last.volume)
        assertEquals(SleepTimerState.Off, timer.state)
    }

    @Test
    fun timeWithoutListeningDoesNotCount() {
        timer.start(SleepTimerMode.Duration(1), fadeMs = 0L)
        timer.advance(0L, null)
        assertEquals(60_000L, (timer.state as SleepTimerState.Running).remainingMs)
    }

    @Test
    fun fadeRampsTheVolumeLinearlyAndAnnouncesItOnce() {
        timer.start(SleepTimerMode.Duration(1), fadeMs = 30_000L)
        val beforeFade = timer.advance(20_000L, null)
        assertEquals(1f, beforeFade.volume)
        assertFalse(beforeFade.hasFadeStarted)

        val fadeStart = timer.advance(15_000L, null)
        assertTrue(fadeStart.hasFadeStarted)
        assertEquals(25f / 30f, fadeStart.volume, 0.001f)
        assertTrue(timer.isFading)

        val middle = timer.advance(10_000L, null)
        assertFalse(middle.hasFadeStarted)
        assertEquals(15f / 30f, middle.volume, 0.001f)

        assertTrue(timer.advance(15_000L, null).shouldStop)
    }

    @Test
    fun boundaryModesFollowTheRemainingTimeToTheBoundary() {
        timer.start(SleepTimerMode.EndOfChapter, fadeMs = 10_000L, boundaryRemainingMs = 120_000L)
        timer.advance(1_000L, 60_000L)
        assertEquals(running(SleepTimerMode.EndOfChapter, 60_000L, false), timer.state)

        assertEquals(0.5f, timer.advance(1_000L, 5_000L).volume, 0.001f)
        assertTrue(timer.advance(1_000L, -200L).shouldStop)
    }

    @Test
    fun unknownBoundaryKeepsTheLastValue() {
        timer.start(SleepTimerMode.EndOfTrack, fadeMs = 0L, boundaryRemainingMs = 90_000L)
        assertFalse(timer.advance(1_000L, null).shouldStop)
        assertEquals(90_000L, (timer.state as SleepTimerState.Running).remainingMs)
    }

    @Test
    fun extendRestartsDurationAndSwitchesBoundaryModesToTheDefault() {
        timer.start(SleepTimerMode.Duration(10), fadeMs = 0L)
        timer.advance(500_000L, null)
        assertTrue(timer.extend(defaultMinutes = 15, onlyDuringFade = false))
        assertEquals(running(SleepTimerMode.Duration(10), 600_000L, false), timer.state)

        timer.start(SleepTimerMode.EndOfChapter, fadeMs = 0L, boundaryRemainingMs = 1_000L)
        assertTrue(timer.extend(defaultMinutes = 15, onlyDuringFade = false))
        assertEquals(running(SleepTimerMode.Duration(15), 900_000L, false), timer.state)
    }

    @Test
    fun extendOnlyDuringFadeIsRefusedBeforeTheFade() {
        timer.start(SleepTimerMode.Duration(1), fadeMs = 30_000L)
        assertFalse(timer.extend(defaultMinutes = 15, onlyDuringFade = true))
        timer.advance(40_000L, null)
        assertTrue(timer.extend(defaultMinutes = 15, onlyDuringFade = true))
        assertEquals(1f, timer.volumeAt((timer.state as SleepTimerState.Running).remainingMs))
    }

    @Test
    fun cancelledOrStoppedTimersIgnoreTicksAndShakes() {
        timer.start(SleepTimerMode.Duration(1), fadeMs = 0L)
        timer.cancel()
        assertEquals(SleepTimer.Tick.IDLE, timer.advance(1_000L, null))
        assertFalse(timer.extend(defaultMinutes = 15, onlyDuringFade = false))
    }

    private fun running(mode: SleepTimerMode, remainingMs: Long, isFading: Boolean) = SleepTimerState.Running(mode, remainingMs, isFading)
}
