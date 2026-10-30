package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber")
object Migration4To5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `equalizer_preset` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `gains_json` TEXT NOT NULL, `built_in` INTEGER NOT NULL)",
        )
        db.execSQL("ALTER TABLE `listening_state` ADD COLUMN `silence_saved_ms` INTEGER NOT NULL DEFAULT 0")
    }
}
