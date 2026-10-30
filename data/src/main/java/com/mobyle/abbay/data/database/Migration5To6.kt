package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Statistics, queue and characters. */
@Suppress("MagicNumber") // Schema versions.
object Migration5To6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_day` (`book_id` INTEGER NOT NULL, `date` TEXT NOT NULL, " +
                "`book_time_ms` INTEGER NOT NULL, `wall_time_ms` INTEGER NOT NULL, `saved_by_speed_ms` INTEGER NOT NULL, " +
                "`saved_by_silence_ms` INTEGER NOT NULL, " +
                "PRIMARY KEY(`book_id`, `date`), FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_day_book_id` ON `listening_day` (`book_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_day_date` ON `listening_day` (`date`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `queue_entry` (`book_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`book_id`), " +
                "FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `character_note` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `book_id` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `first_position_ms` INTEGER, " +
                "FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_character_note_book_id` ON `character_note` (`book_id`)")
    }
}
