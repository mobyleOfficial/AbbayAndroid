package com.mobyle.abbay.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.presentation.booklist.widgets.MiniPlayer
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerActions
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerSkips
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.model.Book
import com.player.PlaybackStatus
import com.player.PlayerState
import com.settings.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The YouTube-style player in its collapsed and expanded states. Possible since INT-04, because
 * the player UI now takes plain state instead of a MediaController.
 */
@OptIn(ExperimentalMaterialApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class MiniPlayerScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val noActions = PlayerActions(
        onPlayButtonClick = {}, onCoverClick = {}, onSeekToBookPosition = {}, onSeekBy = {}, onNextChapter = {},
        onPreviousChapter = {}, onSpeedChange = {}, onApplyBookSpeed = {}, onSetDefaultSpeed = {}, onTrackChanged = {},
        onLockScreen = {}, onOpenDetails = {}, onMarkFinished = {}, onOpenSettings = {},
    )

    private val pausedState = PlayerState(isConnected = true, status = PlaybackStatus.READY)

    private fun capture(
        name: String,
        book: Book,
        expanded: Boolean,
        playerState: PlayerState = pausedState,
        themeMode: ThemeMode = ThemeMode.DARK,
        fontScale: Float = 1f,
    ) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MyApplicationTheme(themeMode = themeMode) {
                    val scaffoldState = rememberBottomSheetScaffoldState(
                        bottomSheetState = rememberBottomSheetState(
                            if (expanded) BottomSheetValue.Expanded else BottomSheetValue.Collapsed,
                        ),
                    )
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = 72.dp,
                        sheetContent = {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                                MiniPlayer(
                                    book = book,
                                    playerState = playerState,
                                    isAwaitingPlayback = false,
                                    isScreenLocked = false,
                                    scaffoldState = scaffoldState,
                                    skips = PlayerSkips(),
                                    actions = noActions,
                                )
                            }
                        },
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun collapsedSingleFileBook() = capture("mini_player_collapsed", SampleBooks.singleFileBook, expanded = false)

    @Test
    fun expandedSingleFileBook() = capture("player_expanded_single_file", SampleBooks.singleFileBook, expanded = true)

    @Test
    fun expandedMultipleFilesBookPlaying() = capture(
        "player_expanded_multiple_files_playing",
        SampleBooks.multipleFilesBook,
        expanded = true,
        playerState = pausedState.copy(isPlaying = true, playWhenReady = true, currentTrackIndex = 1),
    )

    @Test
    fun expandedInLightTheme() = capture("player_expanded_light", SampleBooks.singleFileBook, expanded = true, themeMode = ThemeMode.LIGHT)

    /** Text at 200 %: nothing may be clipped. */
    @Test
    fun expandedAtDoubleFontScale() = capture("player_expanded_font_200", SampleBooks.multipleFilesBook, expanded = true, fontScale = 2f)

    @Test
    fun expandedInBlackTheme() = capture("player_expanded_black", SampleBooks.singleFileBook, expanded = true, themeMode = ThemeMode.BLACK)
}
