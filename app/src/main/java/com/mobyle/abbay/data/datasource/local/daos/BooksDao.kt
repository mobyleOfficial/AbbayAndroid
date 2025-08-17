package com.mobyle.abbay.data.datasource.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mobyle.abbay.data.mappers.toDomain
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BooksDao {
    @Query("SELECT * FROM BookFileEntity")
    fun observeBookFilesList(): Flow<List<BookFileEntity>>

    @Query("SELECT * FROM MultipleBooksEntity")
    fun observeMultipleBooksList(): Flow<List<MultipleBooksEntity>>

    @Query("SELECT * FROM BookFileEntity")
    suspend fun getBookFilesList(): List<BookFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookFilesList(booksList: List<BookFileEntity>)

    @Query("SELECT * FROM MultipleBooksEntity")
    suspend fun getMultipleBooksList(): List<MultipleBooksEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleBooksList(booksList: List<MultipleBooksEntity>)

    @Query("DELETE FROM BookFileEntity WHERE id = :id")
    suspend fun deleteBookFile(id: String)

    @Query("DELETE FROM MultipleBooksEntity WHERE id = :id")
    suspend fun deleteMultipleBook(id: String)

    @Query("DELETE FROM BookFileEntity")
    suspend fun deleteAllBookFiles()

    @Query("DELETE FROM MultipleBooksEntity")
    suspend fun deleteAllMultipleBooks()

    @Query("UPDATE BookFileEntity SET progress = :progress WHERE id = :id")
    suspend fun updateBookFileProgress(id: String, progress: Long)

    @Query("UPDATE MultipleBooksEntity SET progress = :progress, currentBookPosition = :currentPosition WHERE id = :id")
    suspend fun updateMultipleBookProgress(id: String, currentPosition: Int, progress: Long)

    @Transaction
    suspend fun deleteAllBooks(
    ) {
        deleteAllBookFiles()
        deleteAllMultipleBooks()
    }

    @Transaction
    suspend fun upsertBooksList(
        multipleBooksList: List<MultipleBooksEntity>,
        singleFileBooksList: List<BookFileEntity>
    ) {
        deleteAllBooks()
        insertMultipleBooksList(multipleBooksList)
        insertBookFilesList(singleFileBooksList)
    }

    @Transaction
    suspend fun getBooksList(): List<Book> {
        return getBookFilesList().map {
            it.toDomain()
        } + getMultipleBooksList().map {
            it.toDomain()
        }
    }
}
