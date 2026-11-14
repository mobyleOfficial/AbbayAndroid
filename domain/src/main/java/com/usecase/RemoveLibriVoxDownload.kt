package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class RemoveLibriVoxDownload @Inject constructor(private val repository: LibriVoxRepository) {
    suspend operator fun invoke(bookId: String) = repository.removeDownload(bookId)
}
