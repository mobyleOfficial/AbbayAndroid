package com.mobyle.abbay.fakes

import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfPage
import com.model.AudiobookshelfSeries
import com.model.AudiobookshelfSignIn
import com.model.AudiobookshelfSignInError
import com.model.AudiobookshelfSort
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import com.repository.AudiobookshelfRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAudiobookshelfRepository(
    account: AudiobookshelfAccount? = null,
    var librariesResult: DataResult<List<AudiobookshelfLibrary>> = DataResult.Success(emptyList()),
    var booksResult: (libraryId: String, sort: AudiobookshelfSort, page: Int) -> DataResult<AudiobookshelfPage<LibriVoxBook>> =
        { _, _, _ -> DataResult.Success(AudiobookshelfPage(emptyList(), hasMore = false)) },
    var searchResult: DataResult<List<LibriVoxBook>> = DataResult.Success(emptyList()),
    var signInError: AudiobookshelfSignInError? = null,
    var seriesResult: (page: Int) -> DataResult<AudiobookshelfPage<AudiobookshelfSeries>> =
        { DataResult.Success(AudiobookshelfPage(emptyList(), hasMore = false)) },
    var seriesBooksResult: DataResult<List<LibriVoxBook>> = DataResult.Success(emptyList()),
) : AudiobookshelfRepository {
    val account = MutableStateFlow(account)
    val libraryIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val searches = mutableListOf<Pair<String, String>>()
    val bookRequests = mutableListOf<Triple<String, AudiobookshelfSort, Int>>()
    val seriesRequests = mutableListOf<Pair<String, Int>>()
    val seriesBooksRequests = mutableListOf<Pair<String, String>>()
    var detailsFlow: Flow<DataResult<LibriVoxDetails>> = flowOf(DataResult.Failure(DataError.NETWORK))

    override fun observeAccount(): Flow<AudiobookshelfAccount?> = account

    override suspend fun signIn(serverUrl: String, username: String, password: String): AudiobookshelfSignIn {
        signInError?.let { return AudiobookshelfSignIn.Failure(it) }
        val signedIn = AudiobookshelfAccount(serverUrl, username, isSignedIn = true)
        account.value = signedIn
        return AudiobookshelfSignIn.Success(signedIn)
    }

    override suspend fun signOut() {
        account.value = account.value?.copy(isSignedIn = false)
    }

    override suspend fun libraries(): DataResult<List<AudiobookshelfLibrary>> = librariesResult

    override suspend fun books(libraryId: String, sort: AudiobookshelfSort, page: Int): DataResult<AudiobookshelfPage<LibriVoxBook>> =
        booksResult(libraryId, sort, page).also { bookRequests += Triple(libraryId, sort, page) }

    override suspend fun search(libraryId: String, query: String): DataResult<List<LibriVoxBook>> =
        searchResult.also { searches += libraryId to query }

    override suspend fun series(libraryId: String, page: Int): DataResult<AudiobookshelfPage<AudiobookshelfSeries>> =
        seriesResult(page).also { seriesRequests += libraryId to page }

    override suspend fun seriesBooks(libraryId: String, seriesId: String): DataResult<List<LibriVoxBook>> =
        seriesBooksResult.also { seriesBooksRequests += libraryId to seriesId }

    override fun coverUrlOf(itemId: String): String = "https://abs.example.com/api/items/$itemId/cover"

    override fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>> = detailsFlow

    override fun observeLibraryBookIds(): Flow<Map<String, String>> = libraryIds

    override suspend fun add(book: LibriVoxBook, allowsCellular: Boolean, download: Boolean): DataResult<String> {
        val bookId = "book-${book.id}"
        libraryIds.value = libraryIds.value + (book.id to bookId)
        return DataResult.Success(bookId)
    }
}
