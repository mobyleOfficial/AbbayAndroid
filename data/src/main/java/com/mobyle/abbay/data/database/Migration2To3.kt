package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** User overrides of title, author, narrator and description. */
@Suppress("MagicNumber") // Schema versions.
object Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("title_override", "author_override", "narrator_override", "description_override").forEach { column ->
            db.execSQL("ALTER TABLE `book` ADD COLUMN `$column` TEXT")
        }
    }
}
