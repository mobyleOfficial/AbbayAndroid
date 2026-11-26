package com.mobyle.abbay.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration7To8Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AbbayDatabase::class.java)

    @Test
    fun existingBooksFollowTheAppDefaultName() {
        helper.createDatabase(DATABASE, 7).apply {
            execSQL(
                "INSERT INTO book (id, root_id, relative_path, folder_uri, folder_document_id, title, author, narrator, description, " +
                    "cover_uri, cover_source, duration_ms, track_count, is_single_file, added_at, last_scanned_at, " +
                    "folder_last_modified, metadata_complete, hidden, sidecar_id) VALUES (1, NULL, 'a', 'content://a', 'a', " +
                    "'A book', NULL, NULL, NULL, NULL, NULL, 1000, 1, 0, 0, 0, 0, 1, 0, 'sidecar-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 8, true, Migration7To8).apply {
            query("SELECT title, name_source FROM book WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("A book", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
            close()
        }
    }

    private companion object {
        const val DATABASE = "migration-7-8-test"
    }
}
