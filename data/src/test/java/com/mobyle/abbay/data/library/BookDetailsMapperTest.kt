package com.mobyle.abbay.data.library

import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.model.MultipleBooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookDetailsMapperTest {
    private val book = BookEntity(
        id = 1, rootId = 1, relativePath = "Audiobooks/Sea", folderUri = "content://sea", folderDocumentId = "sea",
        title = "Scanned", author = "Tag Author", narrator = null, description = null, coverUri = null, coverSource = null,
        durationMs = 0, trackCount = 3, isSingleFile = false, addedAt = 0, lastScannedAt = 0, folderLastModified = 0,
        sidecarId = "s",
    )

    // Natural order is a, b, c; the track-number tags say c, a, b.
    private val tracks = listOf(
        track(0, "a.mp3", trackNumber = 2),
        track(1, "b.mp3", trackNumber = 3),
        track(2, "c.mp3", trackNumber = 1),
        track(3, "bonus.wma", trackNumber = null, unplayable = true),
    )

    @Test
    fun overridesWinOverScannedTagsAndBlankFieldsShowTheTag() {
        val details = BookWithDetails(book.copy(titleOverride = "Mine", narratorOverride = "Reader"), tracks, null)

        val mapped = BookDetailsMapper.toDetails(details)
        assertEquals("Mine", mapped.title)
        assertEquals("Tag Author", mapped.author)
        assertEquals("Reader", mapped.narrator)
        assertNull(mapped.overrides.author)
        assertEquals("Mine", BookProjection.toBook(details, isRootAccessible = true)?.name)
    }

    @Test
    fun unplayableFilesAreListedAndWmaIsMarkedUnsupported() {
        val mapped = BookDetailsMapper.toDetails(BookWithDetails(book, tracks, null))

        assertEquals(3, mapped.fileCount)
        assertEquals(30_000L, mapped.durationMs)
        assertEquals(listOf("bonus.wma"), mapped.unplayableFiles.map { it.fileName })
        assertEquals(true, mapped.unplayableFiles.single().isUnsupportedFormat)
    }

    @Test
    fun trackNumberOrderFollowsTheTags() {
        val sorted = BookWithDetails(book, tracks, ListeningStateEntity(bookId = 1, sortByTrackTag = true))

        assertEquals(listOf("c.mp3", "a.mp3", "b.mp3"), BookProjection.playableTracks(sorted).map { it.fileName })
        val projected = BookProjection.toBook(sorted, isRootAccessible = true) as MultipleBooks
        assertEquals("c.mp3", projected.bookFileList.first().fileName)
        assertEquals(true, BookDetailsMapper.toDetails(sorted).sortByTrackNumber)
    }

    private fun track(index: Int, name: String, trackNumber: Int?, unplayable: Boolean = false) = TrackEntity(
        bookId = 1, indexInBook = index, documentUri = "content://$name", fileName = name, sizeBytes = 1, lastModified = 0,
        durationMs = 10_000, startInBookMs = index * 10_000L, titleTag = null, trackNumberTag = trackNumber, unplayable = unplayable,
    )
}
