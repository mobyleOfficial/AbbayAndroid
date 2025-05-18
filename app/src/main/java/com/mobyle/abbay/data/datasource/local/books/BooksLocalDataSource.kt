package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity

interface BooksLocalDataSource {
    suspend fun getBookFilesList(): List<BookFileEntity>

    suspend fun addBookFileList(filesList: List<BookFileEntity>)

    suspend fun getMultipleBooksList(): List<MultipleBooksEntity>

    suspend fun addMultipleBooksList(booksList: List<MultipleBooksEntity>)

    suspend fun deleteBook(id: String)

    suspend fun deleteMultipleFilesBook(id: String)

    suspend fun clearBooks()

    fun saveBookFolderPath(path: String)

    fun saveCurrentSelectedBook(id: String?)

    fun getCurrentSelectedBook(): String?

    fun getBooksFolder(): String?
}