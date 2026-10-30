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

@Suppress("TooManyFunctions") // The library contract: roots, scans, books and their metadata.
interface LibraryRepository {
    fun observeRoots(): Flow<List<LibraryRoot>>

    /** Every visible book of every root; hidden books are left out. */
    fun observeBooks(): Flow<List<Book>>

    fun observeScanStatus(): Flow<ScanStatus>

    /** Takes a persistable permission on [treeUri] and adds it as a library root. */
    suspend fun addRoot(treeUri: String): DataResult<LibraryRoot>

    /** Removes the root and its books from the library. Never deletes files. */
    suspend fun removeRoot(rootId: Long)

    /** Adds one audio document picked by the user as a one-file book. */
    suspend fun addSingleFile(documentUri: String): DataResult<Unit>

    /** A file opened from another app as a temporary one-file book, never stored. */
    suspend fun describeExternalFile(documentUri: String): Book?

    /** Re-checks every persisted permission and updates [LibraryRoot.isAccessible]. */
    suspend fun validateRoots()

    /** Quick scan: only folders whose modification time or content changed are read again. */
    suspend fun quickScan()

    /** Full scan in the background (WorkManager). */
    fun requestFullScan()

    /** Runs a full scan in the calling coroutine. Used by the background worker. */
    suspend fun fullScan()

    /** Hides a book: it stays out of the library until it is unhidden; no file is deleted. */
    suspend fun hideBook(bookId: String)

    /** Removes every root and book from the library. Never deletes files. */
    suspend fun clearLibrary()

    /** [pausedAt] is set when the save happens because playback paused (smart rewind). */
    suspend fun saveListeningState(bookId: String, trackIndex: Int, positionInTrackMs: Long, speed: Float, pausedAt: Long? = null)

    suspend fun setBookState(bookId: String, state: BookState)

    fun observeBookDetails(bookId: String): Flow<BookDetails?>

    /** Stores user overrides; audio files are never modified. Blank values clear them. */
    suspend fun updateOverrides(bookId: String, overrides: BookOverrides)

    suspend fun setSortByTrackNumber(bookId: String, enabled: Boolean)

    suspend fun coverCandidates(bookId: String): List<CoverCandidate>

    /** .pdf, .epub, .txt and .fb2 files in the book folder or one level below. */
    suspend fun companionDocuments(bookId: String): List<CompanionDocument>

    suspend fun rememberOpenedDocument(bookId: String, documentUri: String)

    /** Copies the image at [imageUri] as the book's cover (`.abbay/cover.jpg`, or app storage when read-only). */
    suspend fun setUserCover(bookId: String, imageUri: String): DataResult<Unit>
}
