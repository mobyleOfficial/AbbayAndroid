package com.repository

import com.model.Book
import com.model.BookDetails
import com.model.BookOverrides
import com.model.BookState
import com.model.CompanionDocument
import com.model.CoverCandidate
import com.model.DataResult
import com.model.LibraryRoot
import com.model.ScanStatus
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface LibraryRepository {
    fun observeRoots(): Flow<List<LibraryRoot>>

    fun observeBooks(): Flow<List<Book>>

    fun observeScanStatus(): Flow<ScanStatus>

    suspend fun addRoot(treeUri: String): DataResult<LibraryRoot>

    suspend fun removeRoot(rootId: Long)

    suspend fun addSingleFile(documentUri: String): DataResult<Unit>

    suspend fun describeExternalFile(documentUri: String): Book?

    suspend fun validateRoots()

    suspend fun quickScan()

    fun requestFullScan()

    suspend fun fullScan()

    suspend fun hideBook(bookId: String)

    suspend fun clearLibrary()

    suspend fun saveListeningState(bookId: String, trackIndex: Int, positionInTrackMs: Long, speed: Float, pausedAt: Long? = null)

    suspend fun setBookState(bookId: String, state: BookState)

    fun observeBookDetails(bookId: String): Flow<BookDetails?>

    suspend fun updateOverrides(bookId: String, overrides: BookOverrides)

    suspend fun setSortByTrackNumber(bookId: String, enabled: Boolean)

    suspend fun coverCandidates(bookId: String): List<CoverCandidate>

    suspend fun companionDocuments(bookId: String): List<CompanionDocument>

    suspend fun rememberOpenedDocument(bookId: String, documentUri: String)

    suspend fun setUserCover(bookId: String, imageUri: String): DataResult<Unit>
}
