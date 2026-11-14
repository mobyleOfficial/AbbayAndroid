package com.mobyle.abbay.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration3To4Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AbbayDatabase::class.java)

    @Test
    fun bookmarkAndHistoryTablesAreAddedWithoutTouchingProgress() {
        helper.createDatabase(DATABASE, 3).apply {
            execSQL(
                "INSERT INTO book (id, root_id, relative_path, folder_uri, folder_document_id, title, author, narrator, description, " +
                    "cover_uri, cover_source, duration_ms, track_count, is_single_file, added_at, last_scanned_at, " +
                    "folder_last_modified, metadata_complete, hidden, sidecar_id) VALUES (1, NULL, 'a.mp3', 'content://a', 'a', " +
                    "'A book', NULL, NULL, NULL, NULL, NULL, 1000, 1, 1, 0, 0, 0, 1, 0, 'sidecar-1')",
            )
            execSQL(
                "INSERT INTO listening_state (book_id, track_index, position_in_track_ms, book_position_ms, state, speed, " +
                    "pitch_semitones, silence_trim_level, boost_level, mono, balance, noise_reduction_level, repeat_mode, " +
                    "skip_intro_s, skip_outro_s, sort_by_track_tag, updated_at) VALUES (1, 0, 700, 700, 'IN_PROGRESS', 1.0, 0, " +
                    "'OFF', 'OFF', 0, 0, 'OFF', 'OFF', 0, 0, 0, 0)"
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 4, true, Migration3To4).apply {
            execSQL("INSERT INTO bookmark (book_id, book_position_ms, chapter_title, note, created_at) VALUES (1, 500, '', '', 0)")
            execSQL("INSERT INTO history_event (book_id, type, at, position_ms, extra_ms) VALUES (1, 'SEEK', 0, 0, 500)")
            query("SELECT book_position_ms FROM listening_state WHERE book_id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals(700L, cursor.getLong(0))
            }
            execSQL("PRAGMA foreign_keys = ON")
            execSQL("DELETE FROM book WHERE id = 1")
            query("SELECT (SELECT COUNT(*) FROM bookmark) + (SELECT COUNT(*) FROM history_event)").use { cursor ->
                cursor.moveToFirst()
                assertEquals("bookmarks and history follow their book", 0, cursor.getInt(0))
            }
            close()
        }
    }

    private companion object {
        const val DATABASE = "migration-3-4-test"
    }
}
