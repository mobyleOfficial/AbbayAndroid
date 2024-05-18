package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.mappers.toDomain
import com.mobyle.abbay.data.mappers.toEntity
import com.model.Book
import com.model.BookFile
import com.model.BookFolder
import com.repository.BooksRepository
import javax.inject.Inject

class BooksRepositoryImpl @Inject constructor(private val localDataSource: BooksLocalDataSource) :
    BooksRepository {
    override suspend fun getBookList(): List<Book> {
        val bookFilesList = localDataSource.getBookFilesList().map {
            it.toDomain()
        }

        val booksFolderList = localDataSource.getBookFolderList().map {
            it.toDomain()
        }

        return booksFolderList + bookFilesList
    }

    override suspend fun upsertBookList(booksList: List<Book>) {
        val folderList = booksList.filterIsInstance<BookFolder>().map { it.toEntity() }
        val bookFilesList = booksList.filterIsInstance<BookFile>().map { it.toEntity() }

        localDataSource.addBookFileList(bookFilesList)
        localDataSource.addBookFolderList(folderList)
    }
}