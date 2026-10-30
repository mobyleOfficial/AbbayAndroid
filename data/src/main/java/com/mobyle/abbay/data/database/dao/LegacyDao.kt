package com.mobyle.abbay.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.mobyle.abbay.data.database.entity.LegacyBookFileEntity
import com.mobyle.abbay.data.database.entity.LegacyMultipleBooksEntity

@Dao
interface LegacyDao {
    @Query("SELECT * FROM legacy_book_file")
    suspend fun getBookFiles(): List<LegacyBookFileEntity>

    @Query("SELECT * FROM legacy_multiple_books")
    suspend fun getMultipleBooks(): List<LegacyMultipleBooksEntity>

    @Query("DELETE FROM legacy_book_file WHERE id IN (:ids)")
    suspend fun deleteBookFiles(ids: List<String>)

    @Query("DELETE FROM legacy_multiple_books WHERE id IN (:ids)")
    suspend fun deleteMultipleBooks(ids: List<String>)
}
