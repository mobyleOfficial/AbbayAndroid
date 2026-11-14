package com.mobyle.abbay.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration5To6Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AbbayDatabase::class.java)

    @Test
    fun extrasTablesAreAddedAndFollowTheirBook() {
        helper.createDatabase(DATABASE, 5).apply {
            execSQL(
                "INSERT INTO book (id, root_id, relative_path, folder_uri, folder_document_id, title, author, narrator, description, " +
                    "cover_uri, cover_source, duration_ms, track_count, is_single_file, added_at, last_scanned_at, " +
                    "folder_last_modified, metadata_complete, hidden, sidecar_id) VALUES (1, NULL, 'a', 'content://a', 'a', " +
                    "'A book', NULL, NULL, NULL, NULL, NULL, 1000, 1, 0, 0, 0, 0, 1, 0, 'sidecar-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 6, true, Migration5To6).apply {
            execSQL("PRAGMA foreign_keys = ON")
            execSQL("INSERT INTO listening_day VALUES (1, '2026-10-17', 60000, 40000, 20000, 1000)")
            execSQL("INSERT INTO queue_entry VALUES (1, 0)")
            execSQL("INSERT INTO character_note (book_id, name, description, first_position_ms) VALUES (1, 'Mira', 'Cartographer', 300000)")
            query("SELECT book_time_ms, wall_time_ms FROM listening_day").use { cursor ->
                cursor.moveToFirst()
                assertEquals(60_000L, cursor.getLong(0))
                assertEquals(40_000L, cursor.getLong(1))
            }
            execSQL("DELETE FROM book WHERE id = 1")
            for (table in listOf("listening_day", "queue_entry", "character_note")) {
                query("SELECT COUNT(*) FROM $table").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(table, 0, cursor.getInt(0))
                }
            }
            close()
        }
    }

    private companion object {
        const val DATABASE = "migration-5-6-test"
    }
}
