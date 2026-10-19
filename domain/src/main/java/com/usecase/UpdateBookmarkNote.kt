package com.usecase

import com.repository.ListeningRepository
import javax.inject.Inject

class UpdateBookmarkNote @Inject constructor(private val repository: ListeningRepository) {
    suspend operator fun invoke(bookmarkId: Long, note: String) = repository.updateBookmarkNote(bookmarkId, note.trim())
}
