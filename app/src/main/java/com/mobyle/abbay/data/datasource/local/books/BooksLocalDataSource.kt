package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity

interface BooksLocalDataSource {
    suspend fun getBookFilesList(): List<BookFileEntity>

    suspend fun addBookFileList(filesList: List<BookFileEntity>)

    suspend fun getBookFolderList(): List<BookFolderEntity>

    suspend fun addBookFolderList(folderList: List<BookFolderEntity>)
}