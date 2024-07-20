package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import javax.inject.Inject

class BooksLocalDataSourceImpl @Inject constructor(private val booksDao: BooksDao) :
    BooksLocalDataSource {
    override suspend fun getBookFilesList(): List<BookFileEntity> = booksDao.getBookFilesList()

    override suspend fun addBookFileList(filesList: List<BookFileEntity>) =
        booksDao.insertBookFilesList(filesList)

    override suspend fun getMultipleBooksList(): List<MultipleBooksEntity> = booksDao.getMultipleBooksList()

    override suspend fun addMultipleBooksList(booksList: List<MultipleBooksEntity>) =
        booksDao.insertMultipleBooksList(booksList)

    override suspend fun getBooksFolderList(): List<BookFolderEntity> = booksDao.getBooksFolderList()

    override suspend fun addBooksFolderList(folderList: List<BookFolderEntity>) {
        booksDao.insertBooksFolderList(folderList)
    }
}