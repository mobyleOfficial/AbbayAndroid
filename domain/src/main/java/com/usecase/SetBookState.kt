package com.usecase

import com.model.BookState
import com.repository.LibraryRepository
import javax.inject.Inject

class SetBookState @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, state: BookState) = repository.setBookState(bookId, state)
}
