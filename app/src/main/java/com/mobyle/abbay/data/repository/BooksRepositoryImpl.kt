package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.mappers.toDomain
import com.mobyle.abbay.data.mappers.toEntity
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.repository.BooksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

class BooksRepositoryImpl @Inject constructor(
    private val localDataSource: BooksLocalDataSource
) : BooksRepository {

    override fun observeBooksList(): Flow<List<Book>> {
        with(localDataSource) {
            return combine(
                observeMultipleBooksList(),
                observeBookFilesList()
            ) { multipleBooks, bookFile ->
                val multipleBooksDomain = multipleBooks.map {
                    it.toDomain()
                }
                val bookFileDomain = bookFile.map {
                    it.toDomain()
                }

                (multipleBooksDomain + bookFileDomain)
            }
        }
    }

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
        when (book) {
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
    }

    override fun saveBookFolderPath(path: String) = localDataSource.saveBookFolderPath(path)

    override fun saveCurrentSelectedBook(id: String?, position: Int?) =
        localDataSource.saveCurrentSelectedBook(id, position)

    override fun getCurrentSelectedBook() = localDataSource.getCurrentSelectedBook()

    override fun getBooksFolder() = localDataSource.getBooksFolder()
    override fun hasShownReloadGuide() = localDataSource.hasShownReloadGuide()

    override fun setReloadGuideAsShown() = localDataSource.setReloadGuideAsShown()

    override suspend fun updateSelectedBook(progress: Long, position: Int?) =
        localDataSource.updateSelectedBook(progress, position)

    override fun isAppAlive() = localDataSource.isAppAlive()

    override fun updateAppLifeStatus(isAlive: Boolean) =
        localDataSource.updateAppLifeStatus(isAlive)
}