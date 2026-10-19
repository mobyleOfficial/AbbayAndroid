package com.usecase

import com.repository.LibraryRepository
import javax.inject.Inject

class ObserveBookDetails @Inject constructor(private val repository: LibraryRepository) {
    operator fun invoke(bookId: String) = repository.observeBookDetails(bookId)
}
