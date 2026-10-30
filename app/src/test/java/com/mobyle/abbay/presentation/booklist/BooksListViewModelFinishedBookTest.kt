package com.mobyle.abbay.presentation.booklist

import android.app.Application
import com.mobyle.abbay.fakes.FakeLibraryRepository
import com.mobyle.abbay.fakes.FakeSettingsRepository
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.fakes.ViewModelFactory
import com.model.BookFile
import com.model.BookState
import com.player.PlaybackStatus
import com.player.UpNext
import com.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class BooksListViewModelFinishedBookTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val finishedBook = SampleBooks.singleFileBook.copy(state = BookState.FINISHED, progress = SampleBooks.singleFileBook.duration)
    private val inProgressBook = SampleBooks.multipleFilesBook.copy(state = BookState.IN_PROGRESS)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun factory(settings: AppSettings = AppSettings()) = ViewModelFactory(
        libraryRepository = FakeLibraryRepository(listOf(finishedBook, inProgressBook)),
        settingsRepository = FakeSettingsRepository(settings),
    )

    @Test
    fun `a finished book starts over`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()

        val start = viewModel.startingPoint(finishedBook) as BookFile

        assertEquals(0L, start.progress)
        assertEquals(BookState.NEW, start.state)
        assertTrue(factory.libraryRepository.calls.contains("setBookState(${finishedBook.id}, NEW)"))
    }

    @Test
    fun `a book that is not finished keeps its position`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()

        assertSame(inProgressBook, viewModel.startingPoint(inProgressBook))
        assertFalse(factory.libraryRepository.calls.any { it.startsWith("setBookState") })
    }

    @Test
    fun `the end of the book closes the player`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(inProgressBook, 2)

        factory.playerController.mutableState.value = factory.playerController.mutableState.value.copy(status = PlaybackStatus.ENDED)
        dispatcher.scheduler.advanceTimeBy(2_000L)

        assertNull(viewModel.selectedBook.value)
        assertTrue(viewModel.showBookEndedDialog.value)
        assertTrue(factory.playerController.commands.contains("stop"))
        assertNull(factory.settingsRepository.settings.value.lastBookId)
    }

    @Test
    fun `the player stays when the queue goes on with another book`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(inProgressBook, 2)

        factory.playerController.mutableState.value = factory.playerController.mutableState.value.copy(
            status = PlaybackStatus.ENDED,
            upNext = UpNext(bookId = "next", title = "Next", secondsLeft = 3),
        )
        dispatcher.scheduler.advanceTimeBy(2_000L)

        assertEquals(inProgressBook.id, viewModel.selectedBook.value?.id)
        assertFalse(factory.playerController.commands.contains("stop"))
    }

    @Test
    fun `a finished last book is not brought back at start`() {
        val viewModel = factory(AppSettings(lastBookId = finishedBook.id)).booksListViewModel()

        assertNull(viewModel.selectedBook.value)
    }
}
