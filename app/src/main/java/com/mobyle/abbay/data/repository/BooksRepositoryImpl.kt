package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.model.Book
import com.repository.BooksRepository
import javax.inject.Inject

class BooksRepositoryImpl @Inject constructor(private val localDataSource: BooksLocalDataSource) :
    BooksRepository {
    override fun getBooksList(): List<Book> {
        return emptyList()
    }
}