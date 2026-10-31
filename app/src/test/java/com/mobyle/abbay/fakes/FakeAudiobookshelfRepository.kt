package com.mobyle.abbay.fakes

import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfSignIn
import com.model.AudiobookshelfSignInError
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
    var recentResult: (String) -> DataResult<List<LibriVoxBook>> = { DataResult.Success(emptyList()) },
    var searchResult: DataResult<List<LibriVoxBook>> = DataResult.Success(emptyList()),
    var signInError: AudiobookshelfSignInError? = null,
) : AudiobookshelfRepository {
    val account = MutableStateFlow(account)
    val libraryIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val searches = mutableListOf<Pair<String, String>>()
    val recentRequests = mutableListOf<String>()
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

    override suspend fun recentlyAdded(libraryId: String): DataResult<List<LibriVoxBook>> =
        recentResult(libraryId).also { recentRequests += libraryId }

    override suspend fun search(libraryId: String, query: String): DataResult<List<LibriVoxBook>> =
        searchResult.also { searches += libraryId to query }

    override fun coverUrlOf(itemId: String): String = "https://abs.example.com/api/items/$itemId/cover"

    override fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>> = detailsFlow

    override fun observeLibraryBookIds(): Flow<Map<String, String>> = libraryIds

    override suspend fun add(book: LibriVoxBook, allowsCellular: Boolean, download: Boolean): DataResult<String> {
        val bookId = "book-${book.id}"
        libraryIds.value = libraryIds.value + (book.id to bookId)
        return DataResult.Success(bookId)
    }
}
