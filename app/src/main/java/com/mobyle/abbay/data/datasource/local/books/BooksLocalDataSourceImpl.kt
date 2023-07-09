package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity
import javax.inject.Inject

class BooksLocalDataSourceImpl @Inject constructor(private val booksDao: BooksDao) :
    BooksLocalDataSource {
    override suspend fun getBookFilesList(): List<BookFileEntity> = booksDao.getBookFilesList()

    override suspend fun addBookFileList(filesList: List<BookFileEntity>) =
        booksDao.insertBookFilesList(filesList)

    override suspend fun getBookFolderList(): List<BookFolderEntity> = booksDao.getBookFolderList()

    override suspend fun addBookFolderList(folderList: List<BookFolderEntity>) =
        booksDao.insertBookFolderList(folderList)
}