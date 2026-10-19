package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class SetSortByTrackNumber @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, enabled: Boolean) = repository.setSortByTrackNumber(bookId, enabled)
}
