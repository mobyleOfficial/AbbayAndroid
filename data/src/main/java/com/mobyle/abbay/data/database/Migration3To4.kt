package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Bookmarks and history. */
@Suppress("MagicNumber") // Schema versions.
object Migration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmark` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `book_id` INTEGER NOT NULL, " +
                "`book_position_ms` INTEGER NOT NULL, `chapter_title` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmark_book_id` ON `bookmark` (`book_id`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `history_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `book_id` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, `at` INTEGER NOT NULL, `position_ms` INTEGER NOT NULL, `extra_ms` INTEGER, " +
                "FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_event_book_id` ON `history_event` (`book_id`)")
    }
}
