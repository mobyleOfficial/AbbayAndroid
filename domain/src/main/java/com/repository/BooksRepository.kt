package com.repository

import com.model.Book
import kotlinx.coroutines.flow.Flow

interface BooksRepository {

    fun observeBooksList(): Flow<List<Book>>

    suspend fun getBookList(): List<Book>

    suspend fun upsertBookList(booksList: List<Book>)

    suspend fun deleteBook(book: Book)

    suspend fun clearBooks()

    fun saveBookFolderPath(path: String)

    fun saveCurrentSelectedBook(id: String?, position: Int?)

    fun getCurrentSelectedBook(): String?

    fun getBooksFolder(): String?

    fun hasShownReloadGuide(): Boolean

    fun setReloadGuideAsShown()

    suspend fun updateSelectedBook(progress: Long, position: Int?)

    fun isAppAlive(): Boolean

    fun updateAppLifeStatus(isAlive: Boolean)
}
