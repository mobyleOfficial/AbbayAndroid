package com.mobyle.abbay.data.library

import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.model.BookNameSource
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

    @Test
    fun folderBooksFollowTheAppSettingUnlessTheBookChoosesItsOwnSource() {
        val details = BookWithDetails(book, tracks, null)

        assertEquals("Scanned", BookProjection.toBook(details, isRootAccessible = true, nameFromFolder = false)?.name)
        assertEquals("Sea", BookProjection.toBook(details, isRootAccessible = true, nameFromFolder = true)?.name)
        assertEquals("Sea", BookDetailsMapper.toDetails(details, nameFromFolder = true).title)

        val folderBook = BookWithDetails(book.copy(nameSource = BookNameSource.FOLDER.name), tracks, null)
        assertEquals("Sea", BookProjection.toBook(folderBook, isRootAccessible = true, nameFromFolder = false)?.name)

        val metadataBook = BookWithDetails(book.copy(nameSource = BookNameSource.METADATA.name), tracks, null)
        assertEquals("Scanned", BookProjection.toBook(metadataBook, isRootAccessible = true, nameFromFolder = true)?.name)
    }

    @Test
    fun theUsersOwnTitleWinsOverTheFolderName() {
        val details = BookWithDetails(book.copy(titleOverride = "Mine", nameSource = BookNameSource.FOLDER.name), tracks, null)

        assertEquals("Mine", BookProjection.toBook(details, isRootAccessible = true, nameFromFolder = true)?.name)
    }

    @Test
    fun booksDirectlyInTheLibraryFolderAlwaysUseTheirFile() {
        val looseFile = book.copy(relativePath = "Sea.mp3", isSingleFile = true, nameSource = BookNameSource.FOLDER.name)
        val details = BookWithDetails(looseFile, tracks.take(1), null)

        assertEquals("Scanned", BookProjection.toBook(details, isRootAccessible = true, nameFromFolder = true)?.name)
        assertNull(BookDetailsMapper.toDetails(details, nameFromFolder = true).folderName)
    }

    @Test
    fun aBookFolderAtTheTopOfTheLibraryIsNamedAfterItself() {
        val details = BookWithDetails(book.copy(relativePath = "Sea"), tracks, null)

        assertEquals("Sea", BookDetailsMapper.toDetails(details, nameFromFolder = true).folderName)
        assertEquals(BookNameSource.APP_DEFAULT, BookDetailsMapper.toDetails(details).nameSource)
    }

    private fun track(index: Int, name: String, trackNumber: Int?, unplayable: Boolean = false) = TrackEntity(
        bookId = 1, indexInBook = index, documentUri = "content://$name", fileName = name, sizeBytes = 1, lastModified = 0,
        durationMs = 10_000, startInBookMs = index * 10_000L, titleTag = null, trackNumberTag = trackNumber, unplayable = unplayable,
    )
}
