package com.mobyle.abbay.screenshots

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.mobyle.abbay.fakes.FakeSettingsRepository
import com.mobyle.abbay.fakes.ViewModelFactory
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.mobyle.abbay.presentation.settings.SettingsScreen
import com.settings.AppSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings screen with the preserved options, recorded before the Phase 0B refactor. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class SettingsScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModelFactory = ViewModelFactory(
        settingsRepository = FakeSettingsRepository(AppSettings(playWhenAppIsClosed = true, openPlayerOnStartup = false)),
    )

    @Test
    fun settingsScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsScreen(
                    viewModel = viewModelFactory.settingsViewModel(),
                    booksViewModel = viewModelFactory.booksListViewModel(),
                    onStopPlayer = {},
                    close = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/settings_screen.png")
    }

    /** Text at 200 %: labels wrap, controls stay on screen. */
    @Test
    fun settingsScreenAtDoubleFontScale() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MyApplicationTheme {
                    SettingsScreen(
                        viewModel = viewModelFactory.settingsViewModel(),
                        booksViewModel = viewModelFactory.booksListViewModel(),
                        onStopPlayer = {},
                        close = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/settings_screen_font_200.png")
    }
}
