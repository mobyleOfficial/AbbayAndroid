package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber")
object Migration6To7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `book` ADD COLUMN `librivox_id` TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_book_librivox_id` ON `book` (`librivox_id`)")
        db.execSQL("ALTER TABLE `track` ADD COLUMN `remote_url` TEXT")
    }
}
