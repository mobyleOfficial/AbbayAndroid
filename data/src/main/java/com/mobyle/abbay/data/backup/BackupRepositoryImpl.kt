package com.mobyle.abbay.data.backup

import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.dao.ListeningDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.HistoryEventEntity
import com.mobyle.abbay.data.library.displayTitle
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.model.BackupSummary
import com.model.DataError
import com.model.DataResult
import com.model.HistoryEvent
import com.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Caches (covers, thumbnails) are not part of a backup; they are rebuilt by a scan. */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val dao: LibraryDao,
    private val listeningDao: ListeningDao,
    private val sidecarSync: SidecarSync,
    private val files: BackupFileSource,
) : BackupRepository {
    override suspend fun export(destinationUri: String): DataResult<BackupSummary> = withContext(Dispatchers.IO) {
        val entries = dao.getAllBooks().filterNot { it.hidden }.mapNotNull { book ->
            val sidecar = sidecarSync.snapshot(book.id) ?: return@mapNotNull null
            val history = listeningDao.getHistory(book.id).map { BackupCodec.HistoryItem(it.type, it.at, it.positionMs, it.extraMs) }
            BackupCodec.Entry(sidecar, book.relativePath, book.displayTitle, history)
        }
        when (val written = files.write(destinationUri, BackupCodec.write(entries, System.currentTimeMillis()))) {
            is DataResult.Success -> DataResult.Success(BackupSummary(books = entries.size))
            is DataResult.Failure -> written
        }
    }

    override suspend fun import(sourceUri: String): DataResult<BackupSummary> = withContext(Dispatchers.IO) {
        val text = when (val read = files.read(sourceUri)) {
            is DataResult.Success -> read.value
            is DataResult.Failure -> return@withContext read
        }
        val entries = BackupCodec.read(text) ?: return@withContext DataResult.Failure(DataError.INVALID_FORMAT)
        var matched = 0
        entries.forEach { entry ->
            val book = matchingBook(entry) ?: return@forEach
            matched++
            sidecarSync.apply(book.id, entry.book)
            mergeHistory(book.id, entry.history)
            sidecarSync.onBookChanged(book.id)
        }
        DataResult.Success(BackupSummary(books = matched, unmatchedBooks = entries.size - matched))
    }

    /** By sidecar id first, then by relative path. */
    private suspend fun matchingBook(entry: BackupCodec.Entry): BookEntity? =
        dao.getBookBySidecarId(entry.book.id) ?: dao.getBooksByRelativePath(entry.relativePath).firstOrNull()

    /** Events already present (same type and time) are not added twice. */
    private suspend fun mergeHistory(bookId: Long, history: List<BackupCodec.HistoryItem>) {
        val present = listeningDao.getHistory(bookId).map { it.type to it.at }.toSet()
        history.filterNot { (it.type to it.at) in present }.forEach { item ->
            listeningDao.addHistoryEvent(
                HistoryEventEntity(bookId = bookId, type = item.type, at = item.at, positionMs = item.positionMs, extraMs = item.extraMs),
                HistoryEvent.MAX_EVENTS_PER_BOOK,
            )
        }
    }
}
