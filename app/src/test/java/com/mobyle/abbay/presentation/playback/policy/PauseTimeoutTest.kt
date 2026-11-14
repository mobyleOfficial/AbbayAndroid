package com.mobyle.abbay.presentation.playback.policy

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PauseTimeoutTest {
    private val scope = TestScope()
    private var minutes = 30
    private var timeouts = 0
    private val timeout = PauseTimeout(scope, { minutes }, { timeouts++ })

    @Test
    fun `fires after the configured pause`() {
        timeout.onPaused()
        scope.advanceTimeBy(30 * 60_000L - 1)
        scope.runCurrent()
        assertEquals(0, timeouts)
        scope.advanceTimeBy(2)
        scope.runCurrent()
        assertEquals(1, timeouts)
    }

    @Test
    fun `playing again cancels the countdown`() {
        timeout.onPaused()
        scope.advanceTimeBy(20 * 60_000L)
        timeout.onPlaying()
        scope.advanceTimeBy(60 * 60_000L)
        scope.runCurrent()
        assertEquals(0, timeouts)
    }

    @Test
    fun `zero minutes means never`() {
        minutes = 0
        timeout.onPaused()
        scope.advanceTimeBy(24 * 60 * 60_000L)
        scope.runCurrent()
        assertEquals(0, timeouts)
    }
}
