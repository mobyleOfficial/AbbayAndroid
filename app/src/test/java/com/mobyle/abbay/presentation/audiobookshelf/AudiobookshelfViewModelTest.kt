package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import com.mobyle.abbay.fakes.FakeAudiobookshelfRepository
import com.mobyle.abbay.fakes.FakeLibriVoxRepository
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfSignInError
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.usecase.GetAudiobookshelfLibraries
import com.usecase.GetNetworkStatus
import com.usecase.GetRecentAudiobookshelfBooks
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.ObserveAudiobookshelfLibrary
import com.usecase.SearchAudiobookshelf
import com.usecase.SignInAudiobookshelf
import com.usecase.SignOutAudiobookshelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudiobookshelfViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val signedIn = AudiobookshelfAccount("https://abs.example.com", "abe", isSignedIn = true)
    private val books = AudiobookshelfLibrary("books", "Books")
    private val kids = AudiobookshelfLibrary("kids", "Kids")
    private val dune = LibriVoxBook(id = "dune", title = "Dune", authors = "Frank Herbert", creator = null, coverUrl = "")
    private val gruffalo = dune.copy(id = "gruffalo", title = "The Gruffalo")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAudiobookshelfRepository, network: FakeLibriVoxRepository = FakeLibriVoxRepository()) =
        AudiobookshelfViewModel(
            savedStateHandle = SavedStateHandle(),
            observeAccount = ObserveAudiobookshelfAccount(repository),
            signInAudiobookshelf = SignInAudiobookshelf(repository),
            signOutAudiobookshelf = SignOutAudiobookshelf(repository),
            getLibraries = GetAudiobookshelfLibraries(repository),
            getRecentBooks = GetRecentAudiobookshelfBooks(repository),
            searchAudiobookshelf = SearchAudiobookshelf(repository),
            getNetworkStatus = GetNetworkStatus(network),
            observeAudiobookshelfLibrary = ObserveAudiobookshelfLibrary(repository),
        )

    @Test
    fun `signed in, the first library shows its newest books`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(
            account = signedIn,
            librariesResult = DataResult.Success(listOf(books, kids)),
            recentResult = { DataResult.Success(listOf(dune)) },
        )
        val viewModel = viewModel(repository)

        assertEquals("books", viewModel.selectedLibraryId.value)
        assertEquals(AudiobookshelfBrowseState.Recent(listOf(dune)), viewModel.state.value)
    }

    @Test
    fun `picking another library browses and searches it`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(
            account = signedIn,
            librariesResult = DataResult.Success(listOf(books, kids)),
            recentResult = { id -> DataResult.Success(if (id == "kids") listOf(gruffalo) else listOf(dune)) },
            searchResult = DataResult.Success(listOf(gruffalo)),
        )
        val viewModel = viewModel(repository)

        viewModel.selectLibrary("kids")
        assertEquals(AudiobookshelfBrowseState.Recent(listOf(gruffalo)), viewModel.state.value)

        viewModel.setQuery("gruff")
        advanceTimeBy(DEBOUNCE_MS)
        assertEquals(AudiobookshelfBrowseState.Results(listOf(gruffalo)), viewModel.state.value)
        assertEquals(listOf("kids" to "gruff"), repository.searches)
    }

    @Test
    fun `newest books are loaded once per library`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(
            account = signedIn,
            librariesResult = DataResult.Success(listOf(books)),
            recentResult = { DataResult.Success(listOf(dune)) },
        )
        val viewModel = viewModel(repository)

        viewModel.setQuery("x")
        viewModel.setQuery("")

        assertEquals(AudiobookshelfBrowseState.Recent(listOf(dune)), viewModel.state.value)
        assertEquals(listOf("books"), repository.recentRequests)
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
        repository.recentResult = { DataResult.Success(listOf(dune)) }
        viewModel.retry()

        assertEquals(AudiobookshelfBrowseState.Recent(listOf(dune)), viewModel.state.value)
    }

    @Test
    fun `a refused sign in keeps the form with its reason`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(signInError = AudiobookshelfSignInError.WRONG_CREDENTIALS)
        val viewModel = viewModel(repository)

        viewModel.signIn("abs.example.com", "abe", "wrong")

        assertEquals(AudiobookshelfSignInError.WRONG_CREDENTIALS, viewModel.signInError.value)
        assertFalse(viewModel.isSigningIn.value)
        assertNull(viewModel.account.value)
    }

    @Test
    fun `signing in opens the server's libraries`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(
            librariesResult = DataResult.Success(listOf(books)),
            recentResult = { DataResult.Success(listOf(dune)) },
        )
        val viewModel = viewModel(repository)

        viewModel.signIn("https://abs.example.com", "abe", "secret")

        assertEquals(true, viewModel.account.value?.isSignedIn)
        assertNull(viewModel.signInError.value)
        assertEquals(AudiobookshelfBrowseState.Recent(listOf(dune)), viewModel.state.value)
    }

    @Test
    fun `signing out goes back to the form, keeping the server`() = runTest(dispatcher) {
        val repository = FakeAudiobookshelfRepository(account = signedIn, librariesResult = DataResult.Success(listOf(books)))
        val viewModel = viewModel(repository)

        viewModel.signOut()

        assertEquals(signedIn.copy(isSignedIn = false), viewModel.account.value)
        assertEquals(AudiobookshelfBrowseState.Loading, viewModel.state.value)
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
