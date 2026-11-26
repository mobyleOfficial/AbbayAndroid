package com.mobyle.abbay.presentation.playback.media

import android.app.Application
import com.mobyle.abbay.fakes.SampleBooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class BookMediaItemsTest {
    @Test
    fun `a renamed single-file book gets its new title and keeps its track`() {
        val book = SampleBooks.singleFileBook
        val item = BookMediaItems.of(book).first.single()

        val updated = BookMediaItems.withBookInfo(item, book.copy(name = "Renamed", author = "Someone"))!!

        assertEquals("Renamed", updated.mediaMetadata.title)
        assertEquals("Renamed", updated.mediaMetadata.albumTitle)
        assertEquals("Renamed — Someone", updated.mediaMetadata.artist)
        assertEquals(item.localConfiguration, updated.localConfiguration)
        assertEquals(BookMediaItems.chapterStartsOf(item), BookMediaItems.chapterStartsOf(updated))
    }

    @Test
    fun `a renamed folder book keeps each track's file name as its title`() {
        val book = SampleBooks.multipleFilesBook
        val item = BookMediaItems.of(book).first[1]

        val updated = BookMediaItems.withBookInfo(item, book.copy(name = "Renamed"))!!

        assertEquals("chapter_02", updated.mediaMetadata.title)
        assertEquals("Renamed", updated.mediaMetadata.albumTitle)
    }

    @Test
    fun `an unchanged book leaves the item alone`() {
        val book = SampleBooks.multipleFilesBook
        val item = BookMediaItems.of(book).first[0]

        assertNull(BookMediaItems.withBookInfo(item, book))
    }
}
