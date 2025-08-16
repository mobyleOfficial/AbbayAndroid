package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.Book
import kotlinx.coroutines.flow.Flow

interface BooksLocalDataSource {
    fun observeMultipleBooksList(): Flow<List<MultipleBooksEntity>>

    fun observeBookFilesList(): Flow<List<BookFileEntity>>

    suspend fun getBooksList(): List<Book>

    suspend fun upsertBooksList(
        singleFileBooksList: List<BookFileEntity>,
        multipleBooksList: List<MultipleBooksEntity>
    )

    suspend fun deleteBook(id: String)

    suspend fun deleteMultipleFilesBook(id: String)

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