package com.mobyle.abbay.data.datasource.local.books

import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStore
import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStoreKeys
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.Book
import javax.inject.Inject

class BooksLocalDataSourceImpl @Inject constructor(
    private val booksDao: BooksDao,
    private val keyValueStore: KeyValueStore
) :
    BooksLocalDataSource {
    override fun observeMultipleBooksList() = booksDao.observeMultipleBooksList()

    override fun observeBookFilesList() = booksDao.observeBookFilesList()

    override suspend fun upsertBooksList(
        singleFileBooksList: List<BookFileEntity>,
        multipleBooksList: List<MultipleBooksEntity>
    ) {
        booksDao.upsertBooksList(
            multipleBooksList = multipleBooksList,
            singleFileBooksList = singleFileBooksList
        )
    }

    override suspend fun getBooksList(): List<Book> = booksDao.getBooksList()

    override suspend fun deleteBook(id: String) {
        booksDao.deleteBookFile(id)
    }

    override suspend fun deleteMultipleFilesBook(id: String) {
        booksDao.deleteMultipleBook(id)
    }

    override suspend fun clearBooks() {
        booksDao.deleteAllBooks()
        keyValueStore.deleteAllBookInformation()
    }

    override fun saveBookFolderPath(path: String) {
        keyValueStore.storeStringValue(
            KeyValueStoreKeys.BOOK_FOLDER_PATH,
            path
        )
    }

    override fun saveCurrentSelectedBook(id: String?, position: Int?) {
        keyValueStore.storeStringValue(
            KeyValueStoreKeys.LAST_SELECTED_BOOK_ID,
            id
        )

        keyValueStore.storeStringValue(
            KeyValueStoreKeys.BOOK_POSITION,
            position?.toString()
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

    private fun updateCurrentBookPosition(position: Int?) {
        keyValueStore
            .storeStringValue(KeyValueStoreKeys.BOOK_POSITION, position?.toString())
    }

    override suspend fun updateSelectedBook(progress: Long, position: Int?) {
        getCurrentSelectedBook()?.let { id ->
            try {
                updateCurrentBookPosition(position)

                booksDao.updateMultipleBookProgress(
                    id = id,
                    progress = progress,
                    currentPosition = position ?: 0
                )

                booksDao.updateBookFileProgress(
                    id = id,
                    progress = progress,
                )
            } catch (_: Exception) {
                // do nothing
            }
        }
    }

    override fun isAppAlive() = keyValueStore.getBooleanStoredValue(
        KeyValueStoreKeys.APP_LIFE_STATUS,
    )

    override fun updateAppLifeStatus(isAlive: Boolean) {
        keyValueStore.storeBooleanValue(
            KeyValueStoreKeys.APP_LIFE_STATUS,
            isAlive
        )
    }
}