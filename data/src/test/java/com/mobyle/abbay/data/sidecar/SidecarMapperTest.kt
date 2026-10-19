package com.mobyle.abbay.data.sidecar

import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.BookmarkEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SidecarMapperTest {
    private val book = BookEntity(
        id = 1, rootId = 1, relativePath = "Audiobooks/Sea", folderUri = "content://sea", folderDocumentId = "sea",
        title = "Scanned", author = null, narrator = null, description = null, coverUri = null, coverSource = null,
        durationMs = 0, trackCount = 3, isSingleFile = false, addedAt = 0, lastScannedAt = 0, folderLastModified = 0,
        sidecarId = "sidecar-1", titleOverride = "Mine",
    )
    private val tracks = listOf(track(0, "01.mp3"), track(1, "02.mp3"), track(2, "03.mp3"))

    @Test
    fun theDatabaseStateTravelsWithTheTrackFileName() {
        val state =
            ListeningStateEntity(
                bookId = 1,
                trackIndex = 1,
                positionInTrackMs = 4_000L,
                bookPositionMs = 14_000L,
                speed = 1.5f,
                updatedAt = 99L
            )
        val bookmark = BookmarkEntity(id = 7, bookId = 1, bookPositionMs = 3_000L, chapterTitle = "One", note = "n", createdAt = 5L)

        val sidecar = SidecarMapper.toSidecar(BookWithDetails(book, tracks, state), listOf(bookmark), emptyList())

        assertEquals("sidecar-1", sidecar.id)
        assertEquals(SidecarBook.Position("02.mp3", 4_000L, 14_000L), sidecar.position)
        assertEquals(1.5f, sidecar.audio.speed)
        assertEquals(99L, sidecar.updatedAt)
        assertEquals("Mine", sidecar.overrides.title)
        assertEquals(listOf(SidecarBook.Bookmark(3_000L, "One", "n", 5L)), sidecar.bookmarks)
    }

    @Test
    fun aRestoredPositionFindsItsFileEvenWhenTheOrderChanged() {
        val reordered = listOf(track(0, "00 intro.mp3"), track(1, "01.mp3"), track(2, "02.mp3"), track(3, "03.mp3"))
        val sidecar = sidecarAt("02.mp3")

        val restored = SidecarMapper.toListeningState(sidecar, BookWithDetails(book, reordered, null))

        assertEquals(2, restored.trackIndex)
        assertEquals(4_000L, restored.positionInTrackMs)
        assertEquals(1.25f, restored.speed)
        assertEquals("IN_PROGRESS", restored.state)
        assertEquals(50L, restored.updatedAt)
    }

    @Test
    fun aMissingFileKeepsTheBookPositionOnTheFirstTrack() {
        val restored = SidecarMapper.toListeningState(sidecarAt("gone.mp3"), BookWithDetails(book, tracks, null))
        assertEquals(0, restored.trackIndex)
        assertEquals(0L, restored.positionInTrackMs)
        assertEquals(14_000L, restored.bookPositionMs)
    }

    @Test
    fun newerUpdatedAtWins() {
        val sidecar = sidecarAt("02.mp3")
        assertTrue(SidecarMapper.isNewer(sidecar, BookWithDetails(book, tracks, null)))
        assertTrue(SidecarMapper.isNewer(sidecar, BookWithDetails(book, tracks, ListeningStateEntity(bookId = 1, updatedAt = 49L))))
        assertFalse(SidecarMapper.isNewer(sidecar, BookWithDetails(book, tracks, ListeningStateEntity(bookId = 1, updatedAt = 50L))))
    }

    private fun sidecarAt(fileName: String) = SidecarBook(
        id = "sidecar-1",
        updatedAt = 50L,
        position = SidecarBook.Position(fileName, 4_000L, 14_000L),
        state = "IN_PROGRESS",
        lastPlayedAt = 40L,
        lastPausedAt = 45L,
        audio = SidecarBook.Audio(speed = 1.25f),
    )

    private fun track(index: Int, name: String) = TrackEntity(
        bookId = 1, indexInBook = index, documentUri = "content://$name", fileName = name, sizeBytes = 1, lastModified = 0,
        durationMs = 10_000, startInBookMs = index * 10_000L, titleTag = null, trackNumberTag = null, unplayable = false,
    )
}
