package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class SaveListeningState @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, trackIndex: Int, positionInTrackMs: Long, speed: Float, pausedAt: Long? = null) =
        repository.saveListeningState(bookId, trackIndex, positionInTrackMs, speed, pausedAt)
}
