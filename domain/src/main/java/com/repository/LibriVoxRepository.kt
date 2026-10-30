package com.repository

import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import com.model.LibriVoxDownload
import com.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

interface LibriVoxRepository {
    suspend fun popular(): DataResult<List<LibriVoxBook>>

    suspend fun search(query: String): DataResult<List<LibriVoxBook>>

    fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>>

    fun observeLibraryBookIds(): Flow<Map<String, String>>

    fun observeDownloads(): Flow<Map<String, LibriVoxDownload>>

    suspend fun add(book: LibriVoxBook, allowsCellular: Boolean): DataResult<String>

    fun retryDownload(bookId: String, allowsCellular: Boolean)

    fun networkStatus(): NetworkStatus
}
