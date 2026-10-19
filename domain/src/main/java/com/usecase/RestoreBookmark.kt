package com.usecase

import com.model.Bookmark
import com.repository.ListeningRepository
import javax.inject.Inject

class RestoreBookmark @Inject constructor(private val repository: ListeningRepository) {
    suspend operator fun invoke(bookmark: Bookmark) = repository.restoreBookmark(bookmark)
}
