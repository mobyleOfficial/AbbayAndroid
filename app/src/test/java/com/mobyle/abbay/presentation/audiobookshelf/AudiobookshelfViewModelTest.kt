package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import com.mobyle.abbay.fakes.FakeAudiobookshelfRepository
import com.mobyle.abbay.fakes.FakeLibriVoxRepository
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfPage
import com.model.AudiobookshelfSeries
import com.model.AudiobookshelfSort
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.usecase.GetAudiobookshelfBooks
import com.usecase.GetAudiobookshelfLibraries
import com.usecase.GetAudiobookshelfSeries
import com.usecase.GetNetworkStatus
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.ObserveAudiobookshelfLibrary
import com.usecase.SearchAudiobookshelf
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
class AudiobookshelfViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val signedIn = AudiobookshelfAccount("https://abs.example.com", "abe", isSignedIn = true)
    private val books = AudiobookshelfLibrary("books", "Books")
    private val kids = AudiobookshelfLibrary("kids", "Kids")
    private val dune = LibriVoxBook(id = "dune", title = "Dune", authors = "Frank Herbert", creator = null, coverUrl = "")
    private val messiah = dune.copy(id = "messiah", title = "Dune Messiah")
    private val gruffalo = dune.copy(id = "gruffalo", title = "The Gruffalo")
    private val dunes = AudiobookshelfSeries("s1", "Dune", bookCount = 6, coverUrl = "")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun page(vararg items: LibriVoxBook, hasMore: Boolean = false) = DataResult.Success(AudiobookshelfPage(items.toList(), hasMore))

    private fun viewModel(repository: FakeAudiobookshelfRepository) = AudiobookshelfViewModel(
        savedStateHandle = SavedStateHandle(),
        observeAccount = ObserveAudiobookshelfAccount(repository),
        getLibraries = GetAudiobookshelfLibraries(repository),
        getBooks = GetAudiobookshelfBooks(repository),
        getSeries = GetAudiobookshelfSeries(repository),
        searchAudiobookshelf = SearchAudiobookshelf(repository),
        getNetworkStatus = GetNetworkStatus(FakeLibriVoxRepository()),
        observeAudiobookshelfLibrary = ObserveAudiobookshelfLibrary(repository),
    )

    private fun signedInRepository(vararg libraries: AudiobookshelfLibrary) =
        FakeAudiobookshelfRepository(account = signedIn, librariesResult = DataResult.Success(libraries.toList()))

    @Test
    fun `the first library lists all its books, newest first`() = runTest(dispatcher) {
        val repository = signedInRepository(books, kids).apply { booksResult = { _, _, _ -> page(dune) } }
        val viewModel = viewModel(repository)

        assertEquals("books", viewModel.selectedLibraryId.value)
        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune), MoreItems.NONE), viewModel.state.value)
        assertEquals(listOf(Triple("books", AudiobookshelfSort.RECENTLY_ADDED, 0)), repository.bookRequests)
    }

    @Test
    fun `scrolling near the end loads the next page`() = runTest(dispatcher) {
        val repository = signedInRepository(books).apply {
            booksResult = { _, _, page -> if (page == 0) page(dune, hasMore = true) else page(messiah) }
        }
        val viewModel = viewModel(repository)
        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune), MoreItems.AVAILABLE), viewModel.state.value)

        viewModel.loadMore()

        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune, messiah), MoreItems.NONE), viewModel.state.value)
        viewModel.loadMore()
        assertEquals(listOf(0, 1), repository.bookRequests.map { it.third })
    }

    @Test
    fun `a failed later page keeps the books and is tried again`() = runTest(dispatcher) {
        var isDown = true
        val repository = signedInRepository(books).apply {
            booksResult = { _, _, page ->
                when {
                    page == 0 -> page(dune, hasMore = true)
                    isDown -> DataResult.Failure(DataError.NETWORK)
                    else -> page(messiah)
                }
            }
        }
        val viewModel = viewModel(repository)

        viewModel.loadMore()
        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune), MoreItems.FAILED), viewModel.state.value)

        isDown = false
        viewModel.retry()
        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune, messiah), MoreItems.NONE), viewModel.state.value)
    }

    @Test
    fun `another sort lists the books again from their first page, each sort kept`() = runTest(dispatcher) {
        val repository = signedInRepository(books).apply {
            booksResult = { _, sort, _ -> if (sort == AudiobookshelfSort.TITLE) page(messiah, dune) else page(dune, messiah) }
        }
        val viewModel = viewModel(repository)

        viewModel.selectSort(AudiobookshelfSort.TITLE)
        assertEquals(AudiobookshelfBrowseState.Books(listOf(messiah, dune), MoreItems.NONE), viewModel.state.value)

        viewModel.selectSort(AudiobookshelfSort.RECENTLY_ADDED)
        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune, messiah), MoreItems.NONE), viewModel.state.value)
        assertEquals(2, repository.bookRequests.size)
    }

    @Test
    fun `the series tab lists the series page by page, loaded once`() = runTest(dispatcher) {
        val repository = signedInRepository(books).apply {
            booksResult = { _, _, _ -> page(dune) }
            seriesResult = { DataResult.Success(AudiobookshelfPage(listOf(dunes), hasMore = false)) }
        }
        val viewModel = viewModel(repository)

        viewModel.selectTab(AudiobookshelfTab.SERIES)
        assertEquals(AudiobookshelfBrowseState.Series(listOf(dunes), MoreItems.NONE), viewModel.state.value)

        viewModel.selectTab(AudiobookshelfTab.ALL)
        viewModel.selectTab(AudiobookshelfTab.SERIES)
        assertEquals(listOf("books" to 0), repository.seriesRequests)
    }

    @Test
    fun `picking another library browses and searches it`() = runTest(dispatcher) {
        val repository = signedInRepository(books, kids).apply {
            booksResult = { libraryId, _, _ -> if (libraryId == "kids") page(gruffalo) else page(dune) }
            searchResult = DataResult.Success(listOf(gruffalo))
        }
        val viewModel = viewModel(repository)

        viewModel.selectLibrary("kids")
        assertEquals(AudiobookshelfBrowseState.Books(listOf(gruffalo), MoreItems.NONE), viewModel.state.value)

        viewModel.setQuery("gruff")
        advanceTimeBy(DEBOUNCE_MS)
        assertEquals(AudiobookshelfBrowseState.Results(listOf(gruffalo)), viewModel.state.value)
        assertEquals(listOf("kids" to "gruff"), repository.searches)
    }

    @Test
    fun `an empty library says so`() = runTest(dispatcher) {
        val viewModel = viewModel(signedInRepository(books))

        assertEquals(AudiobookshelfBrowseState.NoResults, viewModel.state.value)
    }

    @Test
    fun `a server without book libraries says so`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAudiobookshelfRepository(account = signedIn))

        assertEquals(AudiobookshelfBrowseState.NoLibraries, viewModel.state.value)
    }

    @Test
    fun `an unreachable server can be retried`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(account = signedIn, librariesResult = DataResult.Failure(DataError.NETWORK))
        val viewModel = viewModel(repository)
        assertEquals(AudiobookshelfBrowseState.Failed, viewModel.state.value)

        repository.librariesResult = DataResult.Success(listOf(books))
        repository.booksResult = { _, _, _ -> page(dune) }
        viewModel.retry()

        assertEquals(AudiobookshelfBrowseState.Books(listOf(dune), MoreItems.NONE), viewModel.state.value)
    }

    @Test
    fun `signing out stops browsing`() = runTest(dispatcher) {
        val repository = signedInRepository(books).apply { booksResult = { _, _, _ -> page(dune) } }
        val viewModel = viewModel(repository)

        repository.signOut()

        assertEquals(AudiobookshelfBrowseState.Loading, viewModel.state.value)
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
