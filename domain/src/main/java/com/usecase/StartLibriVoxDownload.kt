package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class StartLibriVoxDownload @Inject constructor(private val repository: LibriVoxRepository) {
    suspend operator fun invoke(bookId: String, allowsCellular: Boolean) = repository.startDownload(bookId, allowsCellular)
}
