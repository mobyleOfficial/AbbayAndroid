package com.mobyle.abbay.presentation.librivox

import androidx.lifecycle.SavedStateHandle
import com.mobyle.abbay.fakes.FakeLibriVoxRepository
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.NetworkStatus
import com.usecase.GetNetworkStatus
import com.usecase.GetPopularLibriVoxBooks
import com.usecase.ObserveLibriVoxLibrary
import com.usecase.SearchLibriVox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibriVoxSearchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val alice = LibriVoxBook(id = "alice", title = "Alice", authors = "Lewis Carroll", creator = "Lewis Carroll", coverUrl = "")
    private val moby = alice.copy(id = "moby", title = "Moby Dick")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeLibriVoxRepository, query: String = "") = LibriVoxSearchViewModel(
        savedStateHandle = SavedStateHandle(mapOf(LibriVoxSearchViewModel.QUERY_ARGUMENT to query)),
        getPopularBooks = GetPopularLibriVoxBooks(repository),
        searchLibriVox = SearchLibriVox(repository),
        getNetworkStatus = GetNetworkStatus(repository),
        observeLibriVoxLibrary = ObserveLibriVoxLibrary(repository),
    )

    @Test
    fun `an empty query shows popular books, loaded once for the screen`() = runTest(dispatcher) {
        val repository =
            FakeLibriVoxRepository(popularResult = DataResult.Success(listOf(alice)), searchResult = DataResult.Success(listOf(moby)))
        val viewModel = viewModel(repository)
        assertEquals(LibriVoxSearchState.Popular(listOf(alice)), viewModel.state.value)

        viewModel.setQuery("moby")
        advanceTimeBy(DEBOUNCE_MS)
        viewModel.setQuery("")

        assertEquals(LibriVoxSearchState.Popular(listOf(alice)), viewModel.state.value)
        assertEquals(1, repository.popularCalls)
    }

    @Test
    fun `typing waits for a pause before searching`() = runTest(dispatcher) {
        val repository = FakeLibriVoxRepository(searchResult = DataResult.Success(listOf(moby)))
        val viewModel = viewModel(repository, query = "")

        viewModel.setQuery("mo")
        viewModel.setQuery("moby")
        assertEquals(LibriVoxSearchState.Loading, viewModel.state.value)
        advanceTimeBy(DEBOUNCE_MS)

        assertEquals(LibriVoxSearchState.Results(listOf(moby)), viewModel.state.value)
        assertEquals(listOf("moby"), repository.searches)
    }

    @Test
    fun `a query passed from the library search runs straight away`() = runTest(dispatcher) {
        val repository = FakeLibriVoxRepository(searchResult = DataResult.Success(emptyList()))
        val viewModel = viewModel(repository, query = "nothing here")
        advanceTimeBy(DEBOUNCE_MS)

        assertEquals(LibriVoxSearchState.NoResults, viewModel.state.value)
    }

    @Test
    fun `offline searches do not reach the network`() = runTest(dispatcher) {
        val repository = FakeLibriVoxRepository(network = NetworkStatus(isOnline = false, isMetered = false))
        val viewModel = viewModel(repository)

        viewModel.setQuery("alice")
        advanceTimeBy(DEBOUNCE_MS)

        assertEquals(LibriVoxSearchState.Offline, viewModel.state.value)
        assertEquals(emptyList<String>(), repository.searches)
    }

    @Test
    fun `a failed popular list can be retried`() = runTest(dispatcher) {
        val repository = FakeLibriVoxRepository(popularResult = DataResult.Failure(DataError.NETWORK))
        val viewModel = viewModel(repository)
        assertEquals(LibriVoxSearchState.Failed, viewModel.state.value)

        repository.popularResult = DataResult.Success(listOf(alice))
        viewModel.retry()

        assertEquals(LibriVoxSearchState.Popular(listOf(alice)), viewModel.state.value)
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
