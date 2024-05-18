package com.usecase

import com.model.Book
import com.repository.BooksRepository
import javax.inject.Inject

class UpsertBookList @Inject constructor(private val repository: BooksRepository) {
    suspend operator fun invoke(booksList: List<Book>) = repository.upsertBookList(booksList)
}