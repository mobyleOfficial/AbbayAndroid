package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class PauseLibriVoxDownload @Inject constructor(private val repository: LibriVoxRepository) {
    suspend operator fun invoke(bookId: String) = repository.pauseDownload(bookId)
}
