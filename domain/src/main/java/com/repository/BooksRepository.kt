package com.repository

import com.model.Book

interface BooksRepository {
    fun getBooksList(): List<Book>
}