package com.mobyle.abbay.presentation.booklist

import android.app.Application
import com.mobyle.abbay.fakes.FakeLibraryRepository
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.fakes.ViewModelFactory
import com.player.PlayerError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class BooksListViewModelPlayerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val singleFileBook = SampleBooks.singleFileBook
    private val multipleFilesBook = SampleBooks.multipleFilesBook
    private val factory = ViewModelFactory(
        libraryRepository = FakeLibraryRepository(listOf(singleFileBook, multipleFilesBook)),
    )
    private val player = factory.playerController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening a book loads it at its saved track and position and plays`() {
        val viewModel = factory.booksListViewModel()

        viewModel.openBook(multipleFilesBook)

        assertEquals(listOf("openBook(201, progress=120000, playWhenReady=true)"), player.commands)
        assertEquals(multipleFilesBook, player.openedBook)
    }

    @Test
    fun `starting a book selects and plays it, and a second tap leaves the playing book alone`() {
        val viewModel = factory.booksListViewModel()

        assertTrue(viewModel.startBook(multipleFilesBook))
        assertTrue(viewModel.startBook(multipleFilesBook))

        assertEquals(multipleFilesBook.id, viewModel.selectedBook.value?.id)
        assertEquals(listOf("openBook(201, progress=120000, playWhenReady=true)"), player.commands)
    }

    @Test
    fun `a book whose file is gone is not started`() {
        val viewModel = factory.booksListViewModel()

        assertFalse(viewModel.startBook(singleFileBook.copy(hasError = true)))

        assertNull(viewModel.selectedBook.value)
        assertTrue(player.commands.isEmpty())
    }

    @Test
    fun `play button pauses while playing`() {
        val viewModel = factory.booksListViewModel()
        player.emitPlaying()

        viewModel.onPlayButtonClicked(singleFileBook, progress = 5_000L)

        assertEquals(listOf("pause"), player.commands)
    }

    @Test
    fun `play button reloads a single file book at the displayed progress and shows loading until ready`() {
        val viewModel = factory.booksListViewModel()

        viewModel.onPlayButtonClicked(singleFileBook, progress = 5_000L)

        assertEquals(listOf("openBook(101, progress=5000, playWhenReady=true)"), player.commands)
        assertTrue(viewModel.isAwaitingPlayback.value)

        player.emitPlaying()

        assertFalse(viewModel.isAwaitingPlayback.value)
    }

    @Test
    fun `play and cover resume a ready player that holds the book without reloading or seeking`() {
        val viewModel = factory.booksListViewModel()
        player.emitPausedAndReady()
        player.mutableState.value = player.mutableState.value.copy(bookId = singleFileBook.id)

        viewModel.onPlayButtonClicked(singleFileBook, progress = 42_000L)
        viewModel.onCoverClicked(singleFileBook, progress = 42_000L)

        assertEquals(listOf("play", "play"), player.commands)
    }

    @Test
    fun `a ready player holding another book reloads the tapped one`() {
        val viewModel = factory.booksListViewModel()
        player.emitPausedAndReady()
        player.mutableState.value = player.mutableState.value.copy(bookId = "another")

        viewModel.onCoverClicked(singleFileBook, progress = 42_000L)

        assertEquals(listOf("openBook(101, progress=42000, playWhenReady=true)"), player.commands)
    }

    @Test
    fun `seek, chapter selection, speed and stop are forwarded`() {
        val viewModel = factory.booksListViewModel()

        viewModel.seekTo(30_000L)
        viewModel.seekToTrack(2)
        viewModel.changeSpeed(1.5f)
        viewModel.stopPlayer()

        assertEquals(listOf("seekTo(30000)", "seekTo(2, 0)", "setSpeed(1.5)", "stop"), player.commands)
    }

    @Test
    fun `progress refresh copies the player position into the selected book while playing`() {
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(singleFileBook, null)
        player.positionMs = 61_000L

        viewModel.refreshCurrentProgress()
        assertEquals("nothing is copied while paused", 0L, viewModel.currentProgress.value)

        player.emitPlaying()
        viewModel.refreshCurrentProgress()
        assertEquals(61_000L, viewModel.currentProgress.value)
    }

    @Test
    fun `a skipped track becomes a message and playback keeps going`() = runTest(testDispatcher) {
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(singleFileBook, null)
        var message: String? = null
        val collector = launch { message = viewModel.skippedTrackMessages.first() }

        player.mutableErrors.emit(PlayerError.TrackSkipped("chapter_02.mp3"))

        withTimeout(1_000L) { collector.join() }
        assertEquals("chapter_02.mp3", message)
        assertFalse(player.commands.contains("stop"))
    }

    @Test
    fun `missing storage stops playback and checks the library folders`() = runTest(testDispatcher) {
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(singleFileBook, null)
        factory.libraryRepository.calls.clear()

        player.mutableErrors.emit(PlayerError.StorageUnavailable)

        assertTrue(player.commands.contains("stop"))
        assertEquals(listOf("validateRoots", "quickScan"), factory.libraryRepository.calls)
    }

    @Test
    fun `playing a book that was just added waits for it to reach the library`() = runTest(testDispatcher) {
        val viewModel = factory.booksListViewModel()
        val requested = async { viewModel.playRequests.first() }

        viewModel.requestPlay(multipleFilesBook.id.replace("201", "301"))
        val added = multipleFilesBook.copy(id = "301")
        factory.libraryRepository.books.value = factory.libraryRepository.books.value + added

        assertEquals("301", withTimeout(1_000L) { requested.await() }.id)
    }

    @Test
    fun `a LibriVox chapter whose file is gone streams from the same position instead of failing`() = runTest(testDispatcher) {
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(multipleFilesBook, null)
        factory.libriVoxRepository.repairs += multipleFilesBook.id
        player.mutableState.value = player.mutableState.value.copy(trackCount = 2)
        player.positionMs = 754_000L
        player.commands.clear()
        factory.libraryRepository.calls.clear()

        player.mutableErrors.emit(PlayerError.StorageUnavailable)

        assertEquals(listOf("openBook(201, progress=120000, playWhenReady=true)", "seekToBookPosition(754000)"), player.commands)
        assertFalse(viewModel.uiState.value.toString().contains("hasError=true"))
        assertEquals(emptyList<String>(), factory.libraryRepository.calls)
    }

    @Test
    fun `a playback error stops the player, unlocks the screen and asks the UI to collapse`() = runTest(testDispatcher) {
        val viewModel = factory.booksListViewModel()
        viewModel.selectBook(singleFileBook, null)
        viewModel.updateIsScreenLocked(true)
        var collapseRequested = false
        val collector = launch { viewModel.collapsePlayerEvents.first().also { collapseRequested = true } }

        player.mutableErrors.emit(PlayerError.BookUnplayable)

        assertTrue(player.commands.contains("stop"))
        assertFalse(viewModel.isScreenLocked.value)
        withTimeout(1_000L) { collector.join() }
        assertTrue(collapseRequested)
    }
}
