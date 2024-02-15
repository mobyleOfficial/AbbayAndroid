package com.repository

import com.model.Book

interface BooksRepository {
    suspend fun getBookList(): List<Book>

    suspend fun upsertBookList(booksList: List<Book>)
}
