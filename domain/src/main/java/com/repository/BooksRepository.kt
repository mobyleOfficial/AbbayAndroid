package com.repository

import com.model.Book

interface BooksRepository {
    suspend fun getBooksList(): List<Book>

    suspend fun addBooksList(booksList: List<Book>)
}