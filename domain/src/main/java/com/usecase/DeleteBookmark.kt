package com.usecase

import com.repository.ListeningRepository
import javax.inject.Inject

class DeleteBookmark @Inject constructor(private val repository: ListeningRepository) {
    suspend operator fun invoke(bookmarkId: Long) = repository.deleteBookmark(bookmarkId)
}
