package com.mobyle.abbay.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.presentation.booklist.widgets.BookItem
import com.mobyle.abbay.presentation.booklist.widgets.BookListTopBar
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.utils.intermediateProgress
import com.mobyle.abbay.presentation.utils.toHHMMSS
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the existing AbbayAndroid components, recorded before the Phase 0B refactor.
 * They must stay identical after it: `./gradlew verifyRoborazziDebug`.
 * API 30 matches the reference device, which has no dynamic color.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class ExistingComponentsScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
                    content()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun bookListTopBarWithoutFolder() = capture("book_list_top_bar_without_folder") {
        BookListTopBar(openFolderSelector = {}, openFileSelector = {}, openSettings = {}, onRefresh = {})
    }

    @Test
    fun bookListTopBarWithFolder() = capture("book_list_top_bar_with_folder") {
        BookListTopBar(
            openFolderSelector = {},
            openFileSelector = {},
            openSettings = {},
            onRefresh = {},
            hasSelectedFolder = true,
        )
    }

    @Test
    fun bookListTopBarRefreshing() = capture("book_list_top_bar_refreshing") {
        BookListTopBar(
            openFolderSelector = {},
            openFileSelector = {},
            openSettings = {},
            onRefresh = {},
            hasSelectedFolder = true,
            isRefreshing = true,
        )
    }

    @Test
    fun bookItems() = capture("book_items") {
        val singleFileBook = SampleBooks.singleFileBook
        val multipleFilesBook = SampleBooks.multipleFilesBook
        val missingFileBook = SampleBooks.missingFileBook
        val intermediaryProgress = multipleFilesBook.bookFileList.intermediateProgress(multipleFilesBook.currentBookPosition)

        BookItem(
            book = singleFileBook,
            isSelected = true,
            progress = singleFileBook.progress.toHHMMSS(),
            intermediaryProgress = 0L,
            currentMediaIndex = 1,
            onClick = {},
        )
        BookItem(
            book = multipleFilesBook,
            isSelected = false,
            progress = (intermediaryProgress + multipleFilesBook.progress).toHHMMSS(),
            intermediaryProgress = intermediaryProgress,
            currentMediaIndex = multipleFilesBook.currentBookPosition + 1,
            onClick = {},
        )
        BookItem(
            book = missingFileBook,
            isSelected = false,
            progress = missingFileBook.progress.toHHMMSS(),
            intermediaryProgress = 0L,
            currentMediaIndex = 1,
            onClick = {},
        )
    }

    @Test
    fun actionDialog() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AbbayActionDialog(
                    title = "Delete Book",
                    body = "Are you sure you want to delete this book?",
                    actionButtonTitle = "Delete",
                    onDismiss = {},
                    onAction = {},
                )
            }
        }
        captureScreenRoboImage("src/test/screenshots/action_dialog.png")
    }
}
