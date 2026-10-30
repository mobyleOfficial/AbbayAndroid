package com.usecase

import com.model.Bookmark
import com.model.HistoryEventType
import com.repository.ListeningRepository
import javax.inject.Inject

class AddBookmark @Inject constructor(private val repository: ListeningRepository) {
    suspend operator fun invoke(bookId: String, bookPositionMs: Long, now: Long = System.currentTimeMillis()): Bookmark? {
        val bookmark = repository.addBookmark(bookId, bookPositionMs, now) ?: return null
        repository.recordHistoryEvent(bookId, HistoryEventType.BOOKMARK_ADDED, now, bookPositionMs)
        return bookmark
    }
}
