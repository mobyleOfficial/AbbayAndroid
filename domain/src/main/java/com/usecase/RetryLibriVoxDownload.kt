package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class RetryLibriVoxDownload @Inject constructor(private val repository: LibriVoxRepository) {
    operator fun invoke(bookId: String, allowsCellular: Boolean) = repository.retryDownload(bookId, allowsCellular)
}
