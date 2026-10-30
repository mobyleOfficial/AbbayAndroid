package com.mobyle.abbay.fakes

import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import com.model.LibriVoxDownload
import com.model.NetworkStatus
import com.repository.LibriVoxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeLibriVoxRepository(
    var popularResult: DataResult<List<LibriVoxBook>> = DataResult.Success(emptyList()),
    var searchResult: DataResult<List<LibriVoxBook>> = DataResult.Success(emptyList()),
    var detailsFlow: Flow<DataResult<LibriVoxDetails>> = flowOf(DataResult.Failure(DataError.NETWORK)),
    var network: NetworkStatus = NetworkStatus(isOnline = true, isMetered = false),
) : LibriVoxRepository {
    val libraryIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloads = MutableStateFlow<Map<String, LibriVoxDownload>>(emptyMap())
    val searches = mutableListOf<String>()
    val added = mutableListOf<Pair<LibriVoxBook, Boolean>>()
    val retried = mutableListOf<Pair<String, Boolean>>()
    var popularCalls = 0

    override suspend fun popular(): DataResult<List<LibriVoxBook>> = popularResult.also { popularCalls++ }

    override suspend fun search(query: String): DataResult<List<LibriVoxBook>> = searchResult.also { searches += query }

    override fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>> = detailsFlow

    override fun observeLibraryBookIds(): Flow<Map<String, String>> = libraryIds

    override fun observeDownloads(): Flow<Map<String, LibriVoxDownload>> = downloads

    override suspend fun add(book: LibriVoxBook, allowsCellular: Boolean): DataResult<String> {
        added += book to allowsCellular
        val bookId = "book-${book.id}"
        libraryIds.value = libraryIds.value + (book.id to bookId)
        return DataResult.Success(bookId)
    }

    override fun retryDownload(bookId: String, allowsCellular: Boolean) {
        retried += bookId to allowsCellular
    }

    override fun networkStatus(): NetworkStatus = network
}
