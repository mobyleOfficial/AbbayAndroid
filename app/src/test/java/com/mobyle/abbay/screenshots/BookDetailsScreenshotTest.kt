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
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.presentation.bookdetails.BookDetailsContent
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.model.BookDetails
import com.model.BookOverrides
import com.model.UnplayableFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class BookDetailsScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookDetails() {
        val book = SampleBooks.multipleFilesBook
        val details = BookDetails(
            id = book.id,
            title = book.name,
            author = "A. Writer",
            narrator = null,
            description = null,
            coverUri = null,
            durationMs = book.duration,
            fileCount = book.bookFileList.size,
            location = "Audiobooks/Stories of the Sea",
            chapters = book.chapters,
            unplayableFiles = listOf(UnplayableFile("bonus.wma", isUnsupportedFormat = true)),
            sortByTrackNumber = false,
            overrides = BookOverrides(),
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)) {
                    BookDetailsContent(details, emptyList(), {}, {}, {}, {}, {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/book_details.png")
    }
}
