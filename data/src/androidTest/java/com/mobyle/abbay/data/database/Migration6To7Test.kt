package com.mobyle.abbay.data.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration6To7Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AbbayDatabase::class.java)

    @Test
    fun existingBooksHaveNoLibriVoxIdAndTheIdIsUnique() {
        helper.createDatabase(DATABASE, 6).apply {
            execSQL(insertBook(id = 1, sidecarId = "sidecar-1"))
            execSQL(
                "INSERT INTO track (id, book_id, index_in_book, document_uri, file_name, size_bytes, last_modified, duration_ms, " +
                    "start_in_book_ms, title_tag, track_number_tag, unplayable, metadata_read) VALUES (1, 1, 0, 'content://a/1', " +
                    "'1.mp3', 10, 0, 1000, 0, NULL, NULL, 0, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 7, true, Migration6To7).apply {
            query("SELECT librivox_id FROM book WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
            }
            query("SELECT remote_url FROM track WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertTrue(cursor.isNull(0))
            }
            execSQL("UPDATE book SET librivox_id = 'alice_librivox' WHERE id = 1")
            execSQL(insertBook(id = 2, sidecarId = "sidecar-2"))
            val duplicate = runCatching { execSQL("UPDATE book SET librivox_id = 'alice_librivox' WHERE id = 2") }
            assertTrue(duplicate.exceptionOrNull() is SQLiteConstraintException)
            query("SELECT COUNT(*) FROM book WHERE librivox_id IS NULL").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            close()
        }
    }

    private fun insertBook(id: Int, sidecarId: String) =
        "INSERT INTO book (id, root_id, relative_path, folder_uri, folder_document_id, title, author, narrator, description, " +
            "cover_uri, cover_source, duration_ms, track_count, is_single_file, added_at, last_scanned_at, " +
            "folder_last_modified, metadata_complete, hidden, sidecar_id) VALUES ($id, NULL, 'a$id', 'content://a', 'a', " +
            "'A book', NULL, NULL, NULL, NULL, NULL, 1000, 1, 0, 0, 0, 0, 1, 0, '$sidecarId')"

    private companion object {
        const val DATABASE = "migration-6-7-test"
    }
}
