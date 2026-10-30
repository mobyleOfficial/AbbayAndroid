package com.mobyle.abbay.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mobyle.abbay.data.database.AbbayDatabase
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ChapterEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.mobyle.abbay.data.sidecar.SidecarFileSource
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.mobyle.abbay.data.storage.SafDocumentTreeDataSource
import com.model.Bookmark
import com.model.HistoryEvent
import com.model.HistoryEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ListeningRepositoryImplTest {
    private lateinit var database: AbbayDatabase
    private lateinit var repository: ListeningRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AbbayDatabase::class.java)
            .build()
        repository = ListeningRepositoryImpl(database.listeningDao(), database.libraryDao(), sidecarSync())
        val dao = database.libraryDao()
        dao.insertBook(
            BookEntity(
                id = BOOK_ID,
                rootId = null,
                relativePath = "book",
                folderUri = "content://book",
                folderDocumentId = "book",
                title = "Book",
                author = null,
                narrator = null,
                description = null,
                coverUri = null,
                coverSource = null,
                durationMs = 600_000L,
                trackCount = 1,
                isSingleFile = true,
                addedAt = 0L,
                lastScannedAt = 0L,
                folderLastModified = 0L,
                sidecarId = "sidecar",
            ),
        )
        dao.insertTracks(
            listOf(
                TrackEntity(
                    bookId = BOOK_ID,
                    indexInBook = 0,
                    documentUri = "content://a",
                    fileName = "a.mp3",
                    sizeBytes = 1L,
                    lastModified = 0L,
                    durationMs = 600_000L,
                    startInBookMs = 0L,
                    titleTag = null,
                    trackNumberTag = null,
                    unplayable = false,
                ),
            ),
        )
        dao.insertChapters(
            listOf(
                ChapterEntity(bookId = BOOK_ID, indexInBook = 0, title = "Opening", startMs = 0L, endMs = 300_000L, source = "MP4"),
                ChapterEntity(bookId = BOOK_ID, indexInBook = 1, title = "Storm", startMs = 300_000L, endMs = 600_000L, source = "MP4"),
            ),
        )
    }

    @After
    fun tearDown() = database.close()

    /** The test books have no root, so nothing is written to disk. */
    private fun sidecarSync(): SidecarSync {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val contentSource = DocumentContentDataSource(context)
        val settings = SettingsDataSource(
            PreferenceDataStoreFactory.create(produceFile = { File(context.cacheDir, "sidecar-test-${System.nanoTime()}.preferences_pb") }),
        )
        val files = SidecarFileSource(context, SafDocumentTreeDataSource(context), contentSource)
        return SidecarSync(database.libraryDao(), database.listeningDao(), database.extrasDao(), files, settings)
    }

    @Test
    fun bookmarksTakeTheChapterTitleAndSortByPosition() = runBlocking {
        repository.addBookmark(BOOK, 400_000L, createdAt = 1L)
        val first = repository.addBookmark(BOOK, 10_000L, createdAt = 2L)!!
        repository.updateBookmarkNote(first.id, "x".repeat(Bookmark.MAX_NOTE_LENGTH + 10))

        val bookmarks = repository.observeBookmarks(BOOK).first()
        assertEquals(listOf("Opening", "Storm"), bookmarks.map { it.chapterTitle })
        assertEquals(Bookmark.MAX_NOTE_LENGTH, bookmarks.first().note.length)

        repository.deleteBookmark(first.id)
        assertEquals(1, repository.observeBookmarks(BOOK).first().size)
        repository.restoreBookmark(bookmarks.first())
        assertEquals(bookmarks, repository.observeBookmarks(BOOK).first())
    }

    @Test
    fun quickSeeksMergeAndHistoryKeepsTheNewestEvents() = runBlocking {
        repository.recordHistoryEvent(BOOK, HistoryEventType.SEEK, at = 1_000L, positionMs = 0L, targetPositionMs = 30_000L)
        repository.recordHistoryEvent(BOOK, HistoryEventType.SEEK, at = 2_000L, positionMs = 30_000L, targetPositionMs = 60_000L)
        val merged = repository.observeHistory(BOOK).first().single()
        assertEquals(0L, merged.positionMs)
        assertEquals(60_000L, merged.targetPositionMs)

        repeat(HistoryEvent.MAX_EVENTS_PER_BOOK + 5) { index ->
            repository.recordHistoryEvent(BOOK, HistoryEventType.PLAY, at = 10_000L + index, positionMs = index.toLong())
        }
        val history = repository.observeHistory(BOOK).first()
        assertEquals(HistoryEvent.MAX_EVENTS_PER_BOOK, history.size)
        assertEquals(HistoryEvent.MAX_EVENTS_PER_BOOK + 4L, history.first().positionMs)
    }

    private companion object {
        const val BOOK_ID = 1L
        const val BOOK = "1"
    }
}
