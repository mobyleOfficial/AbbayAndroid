package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber")
object Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("title_override", "author_override", "narrator_override", "description_override").forEach { column ->
            db.execSQL("ALTER TABLE `book` ADD COLUMN `$column` TEXT")
        }
    }
}
