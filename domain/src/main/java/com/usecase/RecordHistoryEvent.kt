package com.usecase

import com.model.HistoryEventType
import com.repository.ListeningRepository
import javax.inject.Inject

class RecordHistoryEvent @Inject constructor(private val repository: ListeningRepository) {
    suspend operator fun invoke(
        bookId: String,
        type: HistoryEventType,
        positionMs: Long,
        targetPositionMs: Long? = null,
        at: Long = System.currentTimeMillis(),
    ) = repository.recordHistoryEvent(bookId, type, at, positionMs, targetPositionMs)
}
