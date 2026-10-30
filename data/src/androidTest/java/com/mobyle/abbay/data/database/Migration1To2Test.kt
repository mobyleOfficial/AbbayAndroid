package com.mobyle.abbay.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AbbayDatabase::class.java)

    @Test
    fun legacyRowsSurviveAndNewSchemaValidates() {
        helper.createDatabase(DATABASE, 1).apply {
            execSQL(
                "INSERT INTO BookFileEntity (id, name, fileName, thumbnail, progress, duration, speed, hasError, type) " +
                    "VALUES ('48', 'The Long Road Home', 'long_road_home.m4a', NULL, 61000, 180000, 1.25, 0, 'FILE')",
            )
            execSQL(
                "INSERT INTO MultipleBooksEntity (id, bookFileList, name, thumbnail, progress, duration, currentBookPosition, " +
                    "speed, hasError, type) VALUES ('46', '[]', 'Stories of the Sea', NULL, 50000, 240000, 1, 1.5, 0, 'FOLDER')",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 2, true, Migration1To2).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AbbayDatabase::class.java,
            DATABASE,
        ).addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6).build()
        runBlocking {
            val file = database.legacyDao().getBookFiles().single()
            assertEquals(61_000L, file.progress)
            assertEquals(1.25f, file.speed)
            val multiple = database.legacyDao().getMultipleBooks().single()
            assertEquals(50_000L, multiple.progress)
            assertEquals(1, multiple.currentBookPosition)
            assertEquals(1.5f, multiple.speed)
            assertEquals(0, database.libraryDao().getRoots().size)
        }
        database.close()
    }

    @Test
    fun overrideColumnsAreAddedWithoutTouchingBooks() {
        helper.createDatabase(DATABASE, 2).apply {
            execSQL(
                "INSERT INTO book (id, root_id, relative_path, folder_uri, folder_document_id, title, author, narrator, description, " +
                    "cover_uri, cover_source, duration_ms, track_count, is_single_file, added_at, last_scanned_at, " +
                    "folder_last_modified, metadata_complete, hidden, sidecar_id) VALUES (1, NULL, 'a.mp3', 'content://a', 'a', " +
                    "'Scanned title', NULL, NULL, NULL, NULL, NULL, 1000, 1, 1, 0, 0, 0, 1, 0, 'sidecar-1')",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE, 3, true, Migration2To3).apply {
            query("SELECT title, title_override FROM book WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Scanned title", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
            close()
        }
    }

    private companion object {
        const val DATABASE = "migration-test"
    }
}
