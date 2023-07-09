package com.mobyle.abbay.data.datasource.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity

@Dao
interface BooksDao {
    @Query("SELECT * FROM BookFileEntity")
    suspend fun getBookFilesList(): List<BookFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookFilesList(booksList: List<BookFileEntity>)

    @Query("SELECT * FROM BookFolderEntity")
    suspend fun getBookFolderList(): List<BookFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookFolderList(booksList: List<BookFolderEntity>)

}
