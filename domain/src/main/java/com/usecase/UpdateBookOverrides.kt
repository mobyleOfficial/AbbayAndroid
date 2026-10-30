package com.usecase

import com.model.BookOverrides
import com.repository.LibraryRepository
import javax.inject.Inject

class UpdateBookOverrides @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, overrides: BookOverrides) = repository.updateOverrides(bookId, overrides)
}
