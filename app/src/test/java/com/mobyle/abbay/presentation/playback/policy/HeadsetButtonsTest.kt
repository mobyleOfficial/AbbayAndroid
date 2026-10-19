package com.mobyle.abbay.presentation.playback.policy

import com.mobyle.abbay.presentation.playback.policy.HeadsetButtons.Gesture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeadsetButtonsTest {
    private val scope = TestScope()
    private val gestures = mutableListOf<Gesture>()
    private val buttons = HeadsetButtons(scope) { gestures += it }

    @Test
    fun pressesWithinTheWindowAreCounted() {
        press()
        scope.advanceTimeBy(700)
        assertEquals(listOf(Gesture.SINGLE), gestures)

        press()
        scope.advanceTimeBy(200)
        press()
        scope.advanceTimeBy(700)
        assertEquals(listOf(Gesture.SINGLE, Gesture.DOUBLE), gestures)
    }

    @Test
    fun threePressesActAtOnce() {
        press()
        press()
        press()
        scope.runCurrent()
        assertEquals(listOf(Gesture.TRIPLE), gestures)
        scope.advanceTimeBy(1_000)
        assertEquals(listOf(Gesture.TRIPLE), gestures)
    }

    @Test
    fun holdingTheButtonIsALongPressAndNotAPress() {
        buttons.onDown(repeatCount = 0, isLongPress = false)
        scope.advanceTimeBy(500)
        buttons.onDown(repeatCount = 1, isLongPress = true)
        buttons.onDown(repeatCount = 2, isLongPress = true)
        scope.advanceTimeBy(1_000)
        assertEquals(listOf(Gesture.LONG), gestures)
    }

    private fun press() = buttons.onDown(repeatCount = 0, isLongPress = false)
}
