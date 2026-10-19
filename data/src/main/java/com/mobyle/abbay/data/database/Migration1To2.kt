@file:Suppress("MaxLineLength", "ktlint:standard:max-line-length", "ktlint:standard:argument-list-wrapping")

package com.mobyle.abbay.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 (AbbayAndroid book list) → version 2, per
 * The version 1 tables are kept, renamed, until the legacy progress
 * import has matched them to scanned books; the new library tables start empty and are filled by the first scan.
 * The CREATE statements are the ones Room exported in `schemas/.../2.json`.
 */
object Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `BookFileEntity` RENAME TO `legacy_book_file`")
        db.execSQL("ALTER TABLE `MultipleBooksEntity` RENAME TO `legacy_multiple_books`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `library_root` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tree_uri` TEXT NOT NULL, `display_name` TEXT NOT NULL, `added_at` INTEGER NOT NULL, `is_accessible` INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_library_root_tree_uri` ON `library_root` (`tree_uri`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `book` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `root_id` INTEGER, `relative_path` TEXT NOT NULL, `folder_uri` TEXT NOT NULL, `folder_document_id` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT, `narrator` TEXT, `description` TEXT, `cover_uri` TEXT, `cover_source` TEXT, `duration_ms` INTEGER NOT NULL, `track_count` INTEGER NOT NULL, `is_single_file` INTEGER NOT NULL, `added_at` INTEGER NOT NULL, `last_scanned_at` INTEGER NOT NULL, `folder_last_modified` INTEGER NOT NULL, `metadata_complete` INTEGER NOT NULL DEFAULT 0, `missing_since` INTEGER, `hidden` INTEGER NOT NULL DEFAULT 0, `sidecar_id` TEXT NOT NULL, FOREIGN KEY(`root_id`) REFERENCES `library_root`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_root_id` ON `book` (`root_id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_book_root_id_relative_path` ON `book` (`root_id`, `relative_path`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_book_sidecar_id` ON `book` (`sidecar_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `track` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `book_id` INTEGER NOT NULL, `index_in_book` INTEGER NOT NULL, `document_uri` TEXT NOT NULL, `file_name` TEXT NOT NULL, `size_bytes` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL, `duration_ms` INTEGER NOT NULL, `start_in_book_ms` INTEGER NOT NULL, `title_tag` TEXT, `track_number_tag` INTEGER, `unplayable` INTEGER NOT NULL, `metadata_read` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_book_id` ON `track` (`book_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `chapter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `book_id` INTEGER NOT NULL, `index_in_book` INTEGER NOT NULL, `title` TEXT NOT NULL, `start_ms` INTEGER NOT NULL, `end_ms` INTEGER NOT NULL, `source` TEXT NOT NULL, FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapter_book_id` ON `chapter` (`book_id`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `listening_state` (`book_id` INTEGER NOT NULL, `track_index` INTEGER NOT NULL, `position_in_track_ms` INTEGER NOT NULL, `book_position_ms` INTEGER NOT NULL, `state` TEXT NOT NULL, `last_played_at` INTEGER, `last_paused_at` INTEGER, `speed` REAL NOT NULL, `pitch_semitones` REAL NOT NULL, `silence_trim_level` TEXT NOT NULL, `boost_level` TEXT NOT NULL, `mono` INTEGER NOT NULL, `balance` REAL NOT NULL, `noise_reduction_level` TEXT NOT NULL, `equalizer_json` TEXT, `repeat_mode` TEXT NOT NULL, `loop_a_ms` INTEGER, `loop_b_ms` INTEGER, `skip_intro_s` INTEGER NOT NULL, `skip_outro_s` INTEGER NOT NULL, `sort_by_track_tag` INTEGER NOT NULL, `last_opened_document` TEXT, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`book_id`), FOREIGN KEY(`book_id`) REFERENCES `book`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_state_last_played_at` ON `listening_state` (`last_played_at`)")
    }
}
