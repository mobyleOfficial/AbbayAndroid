package com.mobyle.abbay.infra.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.model.BookEntity

@Database(entities = [BookEntity::class], version = 1)
@TypeConverters(RoomConverters::class)
abstract class AbbayDatabase : RoomDatabase() {
    abstract fun getBooksDao(): BooksDao
}