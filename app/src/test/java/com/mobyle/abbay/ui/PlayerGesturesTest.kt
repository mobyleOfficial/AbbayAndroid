package com.mobyle.abbay.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerCover
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayingState
import com.mobyle.abbay.presentation.booklist.widgets.player.repeatingClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cover gestures and long-press repeat. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class PlayerGesturesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun setCover(gesturesEnabled: Boolean = true) {
        composeTestRule.setContent {
            Box(Modifier.size(300.dp).testTag(COVER)) {
                PlayerCover(
                    thumbnail = null,
                    playerIcon = PlayingState.PAUSED,
                    gesturesEnabled = gesturesEnabled,
                    onTap = { events += "tap" },
                    onDoubleTapLeft = { events += "back" },
                    onDoubleTapRight = { events += "forward" },
                    onSwipeToPrevious = { events += "previous" },
                    onSwipeToNext = { events += "next" },
                )
            }
        }
    }

    @Test
    fun doubleTapOnEachHalfSkips() {
        setCover()
        composeTestRule.onNodeWithTag(COVER).performTouchInput { doubleClick(centerLeft.copy(x = width * 0.25f)) }
        composeTestRule.onNodeWithTag(COVER).performTouchInput { doubleClick(centerRight.copy(x = width * 0.75f)) }
        composeTestRule.waitForIdle()
        assertEquals(listOf("back", "forward"), events)
    }

    @Test
    fun horizontalSwipeChangesChapter() {
        setCover()
        composeTestRule.onNodeWithTag(COVER).performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag(COVER).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(listOf("next", "previous"), events)
    }

    @Test
    fun withoutGesturesOnlyPlayPauseRemains() {
        setCover(gesturesEnabled = false)
        composeTestRule.onNodeWithTag(COVER).performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithTag(COVER).performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        assertEquals(listOf("tap"), events)
    }

    @Test
    fun holdingRepeatsEveryHalfSecond() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Box(Modifier.size(56.dp).repeatingClick("Forward") { events += "skip" })
        }
        val button = composeTestRule.onNodeWithContentDescription("Forward")
        button.performTouchInput { down(center) }
        // Long-press timeout (400 ms in tests) fires once, then every 500 ms: 400, 900, 1400.
        composeTestRule.mainClock.advanceTimeBy(1_600L)
        button.performTouchInput { up() }
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(3, events.size)

        events.clear()
        button.performTouchInput { click() }
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(1, events.size)
    }

    private companion object {
        const val COVER = "cover"
    }
}
