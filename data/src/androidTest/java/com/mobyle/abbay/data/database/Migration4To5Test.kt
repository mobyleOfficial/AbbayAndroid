package com.mobyle.abbay.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Equalizer presets table and the saved-silence column; existing effect settings are untouched. */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AbbayDatabase::class.java)

    @Test
    fun presetsTableAndSavedSilenceColumnAreAdded() {
        helper.createDatabase(DATABASE, 4).apply {
            execSQL(
                "INSERT INTO book (id, root_id, relative_path, folder_uri, folder_document_id, title, author, narrator, description, " +
                    "cover_uri, cover_source, duration_ms, track_count, is_single_file, added_at, last_scanned_at, " +
                    "folder_last_modified, metadata_complete, hidden, sidecar_id) VALUES (1, NULL, 'a', 'content://a', 'a', " +
                    "'A book', NULL, NULL, NULL, NULL, NULL, 1000, 1, 0, 0, 0, 0, 1, 0, 'sidecar-1')",
            )
            execSQL(
                "INSERT INTO listening_state (book_id, track_index, position_in_track_ms, book_position_ms, state, speed, " +
                    "pitch_semitones, silence_trim_level, boost_level, mono, balance, noise_reduction_level, repeat_mode, " +
                    "skip_intro_s, skip_outro_s, sort_by_track_tag, updated_at, equalizer_json) VALUES (1, 0, 700, 700, " +
                    "'IN_PROGRESS', 1.0, 1.5, 'MODERATE', 'LOW', 1, 0, 'OFF', 'OFF', 0, 0, 0, 0, '[0,0,3,0,0,0,0,0,0,0]')",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 5, true, Migration4To5).apply {
            query("SELECT silence_trim_level, boost_level, equalizer_json, silence_saved_ms FROM listening_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals("MODERATE", cursor.getString(0))
                assertEquals("LOW", cursor.getString(1))
                assertEquals("[0,0,3,0,0,0,0,0,0,0]", cursor.getString(2))
                assertEquals(0L, cursor.getLong(3))
            }
            execSQL("INSERT INTO equalizer_preset (name, gains_json, built_in) VALUES ('Mine', '[1,2,3,4,5,6,7,8,9,10]', 0)")
            close()
        }
    }

    private companion object {
        const val DATABASE = "migration-4-5-test"
    }
}
