package com.repository

import com.model.Book
import kotlinx.coroutines.flow.Flow

interface BooksRepository {
    suspend fun getBookList(): List<Book>

    suspend fun upsertBookList(booksList: List<Book>)

    suspend fun deleteBook(book: Book)

    suspend fun clearBooks()

    fun onForceUpdateList(): Flow<Unit>
}
