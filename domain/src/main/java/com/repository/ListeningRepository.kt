package com.repository

import com.model.Bookmark
import com.model.HistoryEvent
import com.model.HistoryEventType
import kotlinx.coroutines.flow.Flow

/** Bookmarks and history of each book. */
interface ListeningRepository {
    /** Bookmarks of [bookId] sorted by position. */
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>>

    /** Adds a bookmark at [bookPositionMs] with the title of the chapter there; null when the book is unknown. */
    suspend fun addBookmark(bookId: String, bookPositionMs: Long, createdAt: Long): Bookmark?

    /** Notes longer than [Bookmark.MAX_NOTE_LENGTH] are cut. */
    suspend fun updateBookmarkNote(bookmarkId: Long, note: String)

    suspend fun deleteBookmark(bookmarkId: Long)

    /** Puts back a deleted bookmark with its id ("Undo"). */
    suspend fun restoreBookmark(bookmark: Bookmark)

    /** History of [bookId], newest first. */
    fun observeHistory(bookId: String): Flow<List<HistoryEvent>>

    /**
     * Adds an event and keeps the newest [HistoryEvent.MAX_EVENTS_PER_BOOK]. Seeks and chapter jumps in quick
     * succession extend the previous entry ([HistoryEvent.shouldMerge]).
     */
    suspend fun recordHistoryEvent(bookId: String, type: HistoryEventType, at: Long, positionMs: Long, targetPositionMs: Long? = null)
}
