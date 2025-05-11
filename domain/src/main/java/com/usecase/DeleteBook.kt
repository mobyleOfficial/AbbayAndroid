package com.usecase

import com.model.Book
import com.repository.BooksRepository
import javax.inject.Inject

class DeleteBook @Inject constructor(
    private val repository: BooksRepository
) {
    suspend operator fun invoke(book: Book) {
        repository.deleteBook(book)
    }
} 