package com.repository

import com.model.Bookmark
import com.model.HistoryEvent
import com.model.HistoryEventType
import kotlinx.coroutines.flow.Flow

interface ListeningRepository {
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>>

    suspend fun addBookmark(bookId: String, bookPositionMs: Long, createdAt: Long): Bookmark?

    suspend fun updateBookmarkNote(bookmarkId: Long, note: String)

    suspend fun deleteBookmark(bookmarkId: Long)

    suspend fun restoreBookmark(bookmark: Bookmark)

    fun observeHistory(bookId: String): Flow<List<HistoryEvent>>

    suspend fun recordHistoryEvent(bookId: String, type: HistoryEventType, at: Long, positionMs: Long, targetPositionMs: Long? = null)
}
