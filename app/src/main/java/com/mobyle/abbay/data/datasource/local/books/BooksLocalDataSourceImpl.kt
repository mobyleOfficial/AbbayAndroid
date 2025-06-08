package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStore
import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStoreKeys
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import javax.inject.Inject

class BooksLocalDataSourceImpl @Inject constructor(
    private val booksDao: BooksDao,
    private val keyValueStore: KeyValueStore
) :
    BooksLocalDataSource {
    override fun observeMultipleBooksList() = booksDao.observeMultipleBooksList()

    override fun observeBookFilesList() = booksDao.observeBookFilesList()

    override suspend fun getBookFilesList(): List<BookFileEntity> = booksDao.getBookFilesList()

    override suspend fun addBookFileList(filesList: List<BookFileEntity>) =
        booksDao.insertBookFilesList(filesList)

    override suspend fun getMultipleBooksList(): List<MultipleBooksEntity> =
        booksDao.getMultipleBooksList()

    override suspend fun addMultipleBooksList(booksList: List<MultipleBooksEntity>) =
        booksDao.insertMultipleBooksList(booksList)

    override suspend fun deleteBook(id: String) {
        booksDao.deleteBookFile(id)
    }

    override suspend fun deleteMultipleFilesBook(id: String) {
        booksDao.deleteMultipleBook(id)
    }

    override suspend fun clearBooks() {
        booksDao.deleteAllBookFiles()
        booksDao.deleteAllMultipleBooks()
        keyValueStore.deleteAllBookInformation()
    }

    override fun saveBookFolderPath(path: String) {
        keyValueStore.storeStringValue(
            KeyValueStoreKeys.BOOK_FOLDER_PATH,
            path
        )
    }

    override fun saveCurrentSelectedBook(id: String?) {
        keyValueStore.storeStringValue(
            KeyValueStoreKeys.LAST_SELECTED_BOOK_ID,
            id
        )
    }

    override fun getCurrentSelectedBook() = keyValueStore
        .getStringStoredValue(KeyValueStoreKeys.LAST_SELECTED_BOOK_ID)

    override fun getBooksFolder() = keyValueStore
        .getStringStoredValue(KeyValueStoreKeys.BOOK_FOLDER_PATH)

    override fun hasShownReloadGuide() = keyValueStore
        .getBooleanStoredValue(KeyValueStoreKeys.RELOAD_GUIDE)

    override fun setReloadGuideAsShown() {
        keyValueStore
            .storeBooleanValue(KeyValueStoreKeys.RELOAD_GUIDE, true)
    }
}