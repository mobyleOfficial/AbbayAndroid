package com.usecase

import com.model.BookState
import com.model.HistoryEventType
import com.repository.LibraryRepository
import com.repository.ListeningRepository
import javax.inject.Inject

class FinishBook @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val listeningRepository: ListeningRepository,
) {
    suspend operator fun invoke(bookId: String, bookPositionMs: Long, now: Long = System.currentTimeMillis()) {
        libraryRepository.setBookState(bookId, BookState.FINISHED)
        listeningRepository.recordHistoryEvent(bookId, HistoryEventType.FINISHED, now, bookPositionMs)
    }
}
