package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.mappers.toDomain
import com.mobyle.abbay.data.mappers.toEntity
import com.mobyle.abbay.data.model.BookEntity
import com.model.Book
import com.repository.BooksRepository
import javax.inject.Inject

class BooksRepositoryImpl @Inject constructor(private val localDataSource: BooksLocalDataSource) :
    BooksRepository {
    override suspend fun getBooksList(): List<Book> {
        return localDataSource.getBooksList().map {
            it.toDomain()
        }
    }

    override suspend fun addBooksList(booksList: List<Book>) {
        localDataSource.addBooksList(booksList.map { it.toEntity() })
    }
}