package com.usecase

import com.model.Book
import com.model.BookFile
import com.repository.BooksRepository
import javax.inject.Inject

class AddBooks @Inject constructor(private val repository: BooksRepository) {
    suspend operator fun invoke(booksList: List<Book>) = repository.addBooks(booksList)
}