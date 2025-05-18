package com.mobyle.abbay.data.repository

import android.util.Log
import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.mappers.toDomain
import com.mobyle.abbay.data.mappers.toEntity
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.repository.BooksRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

class BooksRepositoryImpl @Inject constructor(private val localDataSource: BooksLocalDataSource) :
    BooksRepository {

    private val forceListUpdate = MutableSharedFlow<Unit>(
        replay = 1
    )

    override suspend fun getBookList(): List<Book> {
        val bookFilesList = localDataSource.getBookFilesList().map {
            it.toDomain()
        }

        val booksFolderList = localDataSource.getMultipleBooksList().map {
            it.toDomain()
        }

        return booksFolderList + bookFilesList
    }

    override suspend fun upsertBookList(booksList: List<Book>) {
        val multipleBooksList = booksList.filterIsInstance<MultipleBooks>().map { it.toEntity() }
        val bookFilesList = booksList.filterIsInstance<BookFile>().map { it.toEntity() }

        localDataSource.addBookFileList(bookFilesList)
        localDataSource.addMultipleBooksList(multipleBooksList)
    }

    override suspend fun deleteBook(book: Book) {
        when(book) {
           is MultipleBooks -> {
               localDataSource.deleteMultipleFilesBook(book.id)
           }
           is BookFile -> {
               localDataSource.deleteBook(book.id)
           }
        }
    }

    override suspend fun clearBooks() {
        localDataSource.clearBooks()
        forceListUpdate.tryEmit(Unit)
    }

    override fun onForceUpdateList() = forceListUpdate
}