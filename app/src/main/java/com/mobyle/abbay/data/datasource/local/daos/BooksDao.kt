package com.mobyle.abbay.data.datasource.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
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
}
