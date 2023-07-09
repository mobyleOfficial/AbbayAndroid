package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.model.BookEntity
import com.model.Book

interface BooksLocalDataSource {
    suspend fun getBooksList(): List<BookEntity>

    suspend fun addBooksList(booksList: List<BookEntity>)
}