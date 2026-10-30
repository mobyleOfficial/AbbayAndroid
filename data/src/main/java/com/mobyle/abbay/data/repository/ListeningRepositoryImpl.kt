package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.dao.ListeningDao
import com.mobyle.abbay.data.database.entity.BookmarkEntity
import com.mobyle.abbay.data.database.entity.HistoryEventEntity
import com.mobyle.abbay.data.library.BookProjection
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.model.Bookmark
import com.model.HistoryEvent
import com.model.HistoryEventType
import com.repository.ListeningRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningRepositoryImpl @Inject constructor(
    private val dao: ListeningDao,
    private val libraryDao: LibraryDao,
    private val sidecarSync: SidecarSync,
) : ListeningRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val historyMutex = Mutex()

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> {
        val id = bookId.toLongOrNull() ?: return emptyFlow()
        return dao.observeBookmarks(id).map { bookmarks -> bookmarks.map { it.toDomain() } }
    }

    override suspend fun addBookmark(bookId: String, bookPositionMs: Long, createdAt: Long): Bookmark? = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext null
        val details = libraryDao.getBookWithDetails(id) ?: return@withContext null
        val entity = BookmarkEntity(
            bookId = id,
            bookPositionMs = bookPositionMs.coerceAtLeast(0L),
            chapterTitle = BookProjection.chapterTitleAt(details, bookPositionMs),
            note = "",
            createdAt = createdAt,
        )
        entity.copy(id = dao.insertBookmark(entity)).toDomain().also { sidecarSync.onBookEdited(id) }
    }

    override suspend fun updateBookmarkNote(bookmarkId: Long, note: String) = withContext(ioDispatcher) {
        dao.updateBookmarkNote(bookmarkId, note.take(Bookmark.MAX_NOTE_LENGTH))
        dao.getBookmark(bookmarkId)?.let { sidecarSync.onBookEdited(it.bookId) }
        Unit
    }

    override suspend fun deleteBookmark(bookmarkId: Long) = withContext(ioDispatcher) {
        val bookId = dao.getBookmark(bookmarkId)?.bookId
        dao.deleteBookmark(bookmarkId)
        bookId?.let { sidecarSync.onBookEdited(it) }
        Unit
    }

    override suspend fun restoreBookmark(bookmark: Bookmark) = withContext(ioDispatcher) {
        val bookId = bookmark.bookId.toLongOrNull() ?: return@withContext
        dao.insertBookmark(
            BookmarkEntity(
                id = bookmark.id,
                bookId = bookId,
                bookPositionMs = bookmark.bookPositionMs,
                chapterTitle = bookmark.chapterTitle,
                note = bookmark.note,
                createdAt = bookmark.createdAt,
            ),
        )
        sidecarSync.onBookEdited(bookId)
    }

    override fun observeHistory(bookId: String): Flow<List<HistoryEvent>> {
        val id = bookId.toLongOrNull() ?: return emptyFlow()
        return dao.observeHistory(id).map { events -> events.mapNotNull { it.toDomain() } }
    }

    override suspend fun recordHistoryEvent(bookId: String, type: HistoryEventType, at: Long, positionMs: Long, targetPositionMs: Long?) =
        withContext(ioDispatcher) {
            val id = bookId.toLongOrNull() ?: return@withContext
            historyMutex.withLock {
                val latest = dao.getLatestHistoryEvent(id)
                if (latest != null && HistoryEvent.shouldMerge(latest.toDomain(), type, at)) {
                    dao.updateHistoryEvent(latest.copy(at = at, extraMs = targetPositionMs))
                } else {
                    val event =
                        HistoryEventEntity(bookId = id, type = type.name, at = at, positionMs = positionMs, extraMs = targetPositionMs)
                    dao.addHistoryEvent(event, HistoryEvent.MAX_EVENTS_PER_BOOK)
                }
            }
        }

    private fun BookmarkEntity.toDomain() = Bookmark(id, bookId.toString(), bookPositionMs, chapterTitle, note, createdAt)

    private fun HistoryEventEntity.toDomain(): HistoryEvent? {
        val eventType = HistoryEventType.entries.firstOrNull { it.name == type } ?: return null
        return HistoryEvent(id, bookId.toString(), eventType, at, positionMs, extraMs)
    }
}
