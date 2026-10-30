package com.mobyle.abbay.fakes

import com.model.Bookmark
import com.model.HistoryEvent
import com.model.HistoryEventType
import com.repository.ListeningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeListeningRepository : ListeningRepository {
    val bookmarks = MutableStateFlow(emptyList<Bookmark>())
    val history = MutableStateFlow(emptyList<HistoryEvent>())
    private var nextId = 1L

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarks.map { all -> all.filter { it.bookId == bookId }.sortedBy { it.bookPositionMs } }

    override suspend fun addBookmark(bookId: String, bookPositionMs: Long, createdAt: Long): Bookmark {
        val bookmark = Bookmark(nextId++, bookId, bookPositionMs, chapterTitle = "", note = "", createdAt = createdAt)
        bookmarks.value += bookmark
        return bookmark
    }

    override suspend fun updateBookmarkNote(bookmarkId: Long, note: String) {
        bookmarks.value = bookmarks.value.map { if (it.id == bookmarkId) it.copy(note = note) else it }
    }

    override suspend fun deleteBookmark(bookmarkId: Long) {
        bookmarks.value = bookmarks.value.filterNot { it.id == bookmarkId }
    }

    override suspend fun restoreBookmark(bookmark: Bookmark) {
        bookmarks.value += bookmark
    }

    override fun observeHistory(bookId: String): Flow<List<HistoryEvent>> =
        history.map { all -> all.filter { it.bookId == bookId }.sortedByDescending { it.at } }

    override suspend fun recordHistoryEvent(bookId: String, type: HistoryEventType, at: Long, positionMs: Long, targetPositionMs: Long?) {
        history.value += HistoryEvent(nextId++, bookId, type, at, positionMs, targetPositionMs)
    }
}
