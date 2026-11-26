package com.usecase

import com.model.BookNameSource
import com.repository.LibraryRepository
import javax.inject.Inject

class SetBookNameSource @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(bookId: String, source: BookNameSource) = repository.setBookNameSource(bookId, source)
}
