package com.mobyle.abbay.data.datasource.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobyle.abbay.data.model.BookEntity

@Dao
interface BooksDao {
    @Query("SELECT * FROM BookEntity")
    suspend fun getBooksList(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(booksList: List<BookEntity>)
}
