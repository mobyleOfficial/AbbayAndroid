package com.mobyle.abbay.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.library.LibraryTab
import com.mobyle.abbay.presentation.booklist.widgets.LibraryTabEmptyState
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.settings.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The empty state of each library tab, and the one shown while a scan runs. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class LibraryTabEmptyStateScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(name: String, tab: LibraryTab, isScanning: Boolean = false, themeMode: ThemeMode = ThemeMode.DARK) {
        composeTestRule.setContent {
            MyApplicationTheme(themeMode = themeMode) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)) {
                    LibraryTabEmptyState(tab = tab, isScanning = isScanning, onShowTab = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun inProgress() = capture("library_empty_in_progress", LibraryTab.IN_PROGRESS)

    @Test
    fun new() = capture("library_empty_new", LibraryTab.NEW)

    @Test
    fun finished() = capture("library_empty_finished", LibraryTab.FINISHED)

    @Test
    fun finishedInLightTheme() = capture("library_empty_finished_light", LibraryTab.FINISHED, themeMode = ThemeMode.LIGHT)

    @Test
    fun scanning() = capture("library_empty_scanning", LibraryTab.IN_PROGRESS, isScanning = true)
}
