package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import com.mobyle.abbay.fakes.FakeAudiobookshelfRepository
import com.mobyle.abbay.fakes.FakeLibriVoxRepository
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.NetworkStatus
import com.usecase.GetAudiobookshelfSeriesBooks
import com.usecase.GetNetworkStatus
import com.usecase.ObserveAudiobookshelfLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudiobookshelfSeriesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val dune = LibriVoxBook(id = "dune", title = "Dune", authors = "Frank Herbert", creator = null, coverUrl = "")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAudiobookshelfRepository, network: FakeLibriVoxRepository = FakeLibriVoxRepository()) =
        AudiobookshelfSeriesViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    AudiobookshelfSeriesViewModel.LIBRARY_ARGUMENT to "books",
                    AudiobookshelfSeriesViewModel.SERIES_ARGUMENT to "s1",
                    AudiobookshelfSeriesViewModel.NAME_ARGUMENT to "Dune",
                ),
            ),
            getSeriesBooks = GetAudiobookshelfSeriesBooks(repository),
            getNetworkStatus = GetNetworkStatus(network),
            observeAudiobookshelfLibrary = ObserveAudiobookshelfLibrary(repository),
        )

    @Test
    fun `the series lists its books from its library`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(seriesBooksResult = DataResult.Success(listOf(dune)))
        val viewModel = viewModel(repository)

        assertEquals("Dune", viewModel.name)
        assertEquals(AudiobookshelfSeriesState.Books(listOf(dune)), viewModel.state.value)
        assertEquals(listOf("books" to "s1"), repository.seriesBooksRequests)
    }

    @Test
    fun `offline, the series can be retried`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(seriesBooksResult = DataResult.Failure(DataError.NETWORK))
        val viewModel = viewModel(repository, FakeLibriVoxRepository(network = NetworkStatus(isOnline = false, isMetered = false)))
        assertEquals(AudiobookshelfSeriesState.Offline, viewModel.state.value)

        repository.seriesBooksResult = DataResult.Success(listOf(dune))
        viewModel.load()

        assertEquals(AudiobookshelfSeriesState.Books(listOf(dune)), viewModel.state.value)
    }
}
