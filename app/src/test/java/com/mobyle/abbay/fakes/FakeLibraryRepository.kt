package com.mobyle.abbay.fakes

import com.model.Book
import com.model.BookDetails
import com.model.BookOverrides
import com.model.BookState
import com.model.CompanionDocument
import com.model.CoverCandidate
import com.model.DataResult
import com.model.LibraryRoot
import com.model.ScanStatus
import com.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLibraryRepository(initialBooks: List<Book> = emptyList()) : LibraryRepository {
    val books = MutableStateFlow(initialBooks)
    val roots = MutableStateFlow(emptyList<LibraryRoot>())
    val scanStatus = MutableStateFlow(ScanStatus())
    val calls = mutableListOf<String>()

    override fun observeRoots(): Flow<List<LibraryRoot>> = roots

    override fun observeBooks(): Flow<List<Book>> = books

    override fun observeScanStatus(): Flow<ScanStatus> = scanStatus

    override suspend fun addRoot(treeUri: String): DataResult<LibraryRoot> {
        calls += "addRoot($treeUri)"
        val root = LibraryRoot(roots.value.size + 1L, treeUri, treeUri.substringAfterLast('/'), isAccessible = true)
        roots.value = roots.value + root
        return DataResult.Success(root)
    }

    override suspend fun removeRoot(rootId: Long) {
        calls += "removeRoot($rootId)"
        roots.value = roots.value.filterNot { it.id == rootId }
    }

    override suspend fun addSingleFile(documentUri: String): DataResult<Unit> {
        calls += "addSingleFile($documentUri)"
        return DataResult.Success(Unit)
    }

    override suspend fun describeExternalFile(documentUri: String): Book? = null

    override suspend fun validateRoots() {
        calls += "validateRoots"
    }

    override suspend fun quickScan() {
        calls += "quickScan"
    }

    override fun requestFullScan() {
        calls += "requestFullScan"
    }

    override suspend fun fullScan() {
        calls += "fullScan"
    }

    override suspend fun hideBook(bookId: String) {
        calls += "hideBook($bookId)"
        books.value = books.value.filterNot { it.id == bookId }
    }

    override suspend fun clearLibrary() {
        calls += "clearLibrary"
        books.value = emptyList()
        roots.value = emptyList()
    }

    override suspend fun saveListeningState(bookId: String, trackIndex: Int, positionInTrackMs: Long, speed: Float, pausedAt: Long?) {
        calls += "saveListeningState($bookId, $trackIndex, $positionInTrackMs, $speed)"
    }

    override suspend fun setBookState(bookId: String, state: BookState) {
        calls += "setBookState($bookId, $state)"
    }

    val details = MutableStateFlow<BookDetails?>(null)
    var coverCandidates = emptyList<CoverCandidate>()

    override fun observeBookDetails(bookId: String): Flow<BookDetails?> = details

    override suspend fun updateOverrides(bookId: String, overrides: BookOverrides) {
        calls += "updateOverrides($bookId, $overrides)"
    }

    override suspend fun setSortByTrackNumber(bookId: String, enabled: Boolean) {
        calls += "setSortByTrackNumber($bookId, $enabled)"
    }

    override suspend fun coverCandidates(bookId: String): List<CoverCandidate> = coverCandidates

    var companionDocuments = emptyList<CompanionDocument>()
    val openedDocuments = mutableListOf<Pair<String, String>>()

    override suspend fun companionDocuments(bookId: String): List<CompanionDocument> = companionDocuments

    override suspend fun rememberOpenedDocument(bookId: String, documentUri: String) {
        openedDocuments += bookId to documentUri
    }

    override suspend fun setUserCover(bookId: String, imageUri: String): DataResult<Unit> {
        calls += "setUserCover($bookId, $imageUri)"
        return DataResult.Success(Unit)
    }
}
