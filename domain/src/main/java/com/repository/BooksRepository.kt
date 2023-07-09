package com.repository

import com.model.Book

interface BooksRepository {
    suspend fun getBooks(): List<Book>

    suspend fun addBooks(booksList: List<Book>)
}