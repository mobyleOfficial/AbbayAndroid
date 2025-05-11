package com.mobyle.abbay.infra.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity

@Database(
    entities = [BookFileEntity::class, MultipleBooksEntity::class],
    version = 1
)
abstract class AbbayDatabase : RoomDatabase() {
    abstract fun getBooksDao(): BooksDao
}