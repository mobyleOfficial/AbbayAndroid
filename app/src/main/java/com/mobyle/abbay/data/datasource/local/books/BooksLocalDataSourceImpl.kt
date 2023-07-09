package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.model.BookEntity
import javax.inject.Inject

class BooksLocalDataSourceImpl @Inject constructor(private val booksDao: BooksDao) : BooksLocalDataSource {
    override suspend fun getBooksList() = booksDao.getBooksList()
    override suspend fun addBooksList(booksList: List<BookEntity>) {
        booksDao.insertBooks(booksList)
    }
}