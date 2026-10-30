package com.mobyle.abbay.presentation.librivox

import androidx.lifecycle.SavedStateHandle
import com.mobyle.abbay.fakes.FakeLibriVoxRepository
import com.model.ChapterDownload
import com.model.ChapterDownloadStatus
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import com.model.LibriVoxDownload
import com.model.LibriVoxSection
import com.model.NetworkStatus
import com.usecase.AddLibriVoxBook
import com.usecase.GetNetworkStatus
import com.usecase.ObserveLibriVoxDetails
import com.usecase.ObserveLibriVoxDownloads
import com.usecase.ObserveLibriVoxLibrary
import com.usecase.RetryLibriVoxDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibriVoxBookViewModelTest {
    private val section =
        LibriVoxSection(
            title = "alice_01_64kb",
            durationMs = 60_000L,
            listenUrl = "https://archive.org/download/alice/1.mp3",
            sizeBytes = 1_000L
        )
    private val preview = LibriVoxBook(
        id = "alice",
        title = "Alice",
        authors = "Lewis Carroll",
        creator = "Lewis Carroll",
        coverUrl = "",
        sections = listOf(section),
    )
    private val final = preview.copy(sections = listOf(section.copy(title = "Down the Rabbit-Hole")), description = "A girl falls")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeLibriVoxRepository) = LibriVoxBookViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                LibriVoxBookViewModel.ID_ARGUMENT to "alice",
                LibriVoxBookViewModel.TITLE_ARGUMENT to "Alice",
                LibriVoxBookViewModel.AUTHORS_ARGUMENT to "Lewis Carroll",
                LibriVoxBookViewModel.CREATOR_ARGUMENT to "Lewis Carroll",
            ),
        ),
        observeDetails = ObserveLibriVoxDetails(repository),
        addLibriVoxBook = AddLibriVoxBook(repository),
        retryLibriVoxDownload = RetryLibriVoxDownload(repository),
        getNetworkStatus = GetNetworkStatus(repository),
        observeLibriVoxLibrary = ObserveLibriVoxLibrary(repository),
        observeLibriVoxDownloads = ObserveLibriVoxDownloads(repository),
    )

    @Test
    fun `a download tapped during the preview waits for LibriVox's chapter titles`() {
        val details = MutableSharedFlow<DataResult<LibriVoxDetails>?>(replay = 1)
        val repository = FakeLibriVoxRepository(detailsFlow = details.takeWhileNotNull())
        val viewModel = viewModel(repository)

        details.tryEmit(DataResult.Success(LibriVoxDetails(preview, isAwaitingTitles = true)))
        viewModel.download()
        assertTrue(viewModel.isStarting.value)
        assertEquals(emptyList<Pair<LibriVoxBook, Boolean>>(), repository.added)

        details.tryEmit(DataResult.Success(LibriVoxDetails(final, isAwaitingTitles = false)))
        details.tryEmit(null)

        assertEquals(listOf(final to true), repository.added)
        assertEquals("book-alice", viewModel.libraryBookId.value)
    }

    @Test
    fun `a large book on mobile data asks first, and waiting keeps it on Wi-Fi`() {
        val large = final.copy(sections = listOf(section.copy(sizeBytes = 200_000_000L)))
        val repository = FakeLibriVoxRepository(
            detailsFlow = flowOf(DataResult.Success(LibriVoxDetails(large, isAwaitingTitles = false))),
            network = NetworkStatus(isOnline = true, isMetered = true),
        )
        val viewModel = viewModel(repository)

        viewModel.download()
        assertEquals(200_000_000L, viewModel.cellularPromptBytes.value)
        viewModel.answerCellularPrompt(useMobileData = false)

        assertNull(viewModel.cellularPromptBytes.value)
        assertEquals(listOf(large to false), repository.added)
    }

    @Test
    fun `dismissing the mobile data question downloads nothing`() {
        val large = final.copy(sections = listOf(section.copy(sizeBytes = 200_000_000L)))
        val repository = FakeLibriVoxRepository(
            detailsFlow = flowOf(DataResult.Success(LibriVoxDetails(large, isAwaitingTitles = false))),
            network = NetworkStatus(isOnline = true, isMetered = true),
        )
        val viewModel = viewModel(repository)

        viewModel.download()
        viewModel.answerCellularPrompt(useMobileData = null)

        assertEquals(emptyList<Pair<LibriVoxBook, Boolean>>(), repository.added)
    }

    @Test
    fun `a failed load keeps the search result on screen and can be retried`() {
        val repository = FakeLibriVoxRepository(detailsFlow = flowOf(DataResult.Failure(DataError.NETWORK)))
        val viewModel = viewModel(repository)
        assertEquals(LibriVoxBookState.Failed(viewModel.state.value.book), viewModel.state.value)
        assertEquals("Alice", viewModel.state.value.book.title)

        repository.detailsFlow = flowOf(DataResult.Success(LibriVoxDetails(final, isAwaitingTitles = false)))
        viewModel.load()

        assertEquals(LibriVoxBookState.Loaded(final, isAwaitingTitles = false), viewModel.state.value)
    }

    @Test
    fun `a book in the library shows its download`() {
        val download = LibriVoxDownload.of(listOf(ChapterDownload(ChapterDownloadStatus.DOWNLOADING, 0.5f)), listOf(1_000L))
        val repository = FakeLibriVoxRepository(detailsFlow = flowOf(DataResult.Success(LibriVoxDetails(final, isAwaitingTitles = false))))
        repository.libraryIds.value = mapOf("alice" to "7")
        repository.downloads.value = mapOf("7" to download)
        val viewModel = viewModel(repository)

        assertEquals(download, viewModel.download.value)
        viewModel.download()
        assertEquals(emptyList<Pair<LibriVoxBook, Boolean>>(), repository.added)

        viewModel.retryFailedChapters()
        assertEquals(listOf("7" to true), repository.retried)
    }

    private fun MutableSharedFlow<DataResult<LibriVoxDetails>?>.takeWhileNotNull() = transformWhile { result ->
        result?.let { emit(it) }
        result != null
    }
}
