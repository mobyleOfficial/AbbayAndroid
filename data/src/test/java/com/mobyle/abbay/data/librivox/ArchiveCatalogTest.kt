package com.mobyle.abbay.data.librivox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveCatalogTest {
    @Test
    fun theByAuthorSuffixIsDroppedOnlyForTheItemsOwnCreator() {
        assertEquals(
            "Alice's Adventures in Wonderland",
            CatalogText.cleanTitle("Alice's Adventures in Wonderland, by Lewis Carroll", "Lewis Carroll")
        )
        assertEquals("Stories, by Others", CatalogText.cleanTitle("Stories, by Others", "Lewis Carroll"))
    }

    @Test
    fun lengthsAreColonSeparatedOrSeconds() {
        assertEquals(640_000L, ArchiveCatalog.millis("10:40"))
        assertEquals(3_723_000L, ArchiveCatalog.millis("1:02:03"))
        assertEquals(640_500L, ArchiveCatalog.millis("640.5"))
        assertNull(ArchiveCatalog.millis("ten"))
        assertNull(ArchiveCatalog.millis(""))
    }

    @Test
    fun lastNamesFollowLibriVoxsAuthorFilter() {
        assertEquals("Carroll", CatalogText.lastName("Lewis Carroll"))
        assertEquals("Carroll", CatalogText.lastName("Carroll, Lewis"))
        assertEquals("King", CatalogText.lastName("Martin Luther King, Jr."))
        assertNull(CatalogText.lastName("  "))
    }

    @Test
    fun chapterFilesAreThe64kMp3sInTrackOrder() {
        val files = listOf(
            file("ch_10_64kb.mp3", track = "10/12"),
            file("ch_02_64kb.mp3", track = "2/12"),
            file("ch_01.mp3", track = "1", format = "VBR MP3"),
            file("ch_01_64kb.mp3", track = "1/12"),
        )
        assertEquals(listOf("ch_01_64kb.mp3", "ch_02_64kb.mp3", "ch_10_64kb.mp3"), ArchiveCatalog.chapterFiles(files).map { it.name })
    }

    @Test
    fun withoutALibriVoxMatchChaptersComeFromTheFiles() {
        val item =
            item(files = listOf(file("wonderland_ch_01_64kb.mp3", track = "1/2", title = "Chapter 1"), file("b_64kb.mp3", track = "2/2")))

        val book = ArchiveCatalog.book(item, match = null)!!
        assertEquals(listOf("Chapter 1", "b_64kb"), book.sections.map { it.title })
        assertEquals("https://archive.org/download/alice/wonderland_ch_01_64kb.mp3", book.sections.first().listenUrl)
        assertEquals(1_280_000L, book.durationMs)
        assertEquals(2_000L, book.totalBytes)
        assertEquals("Archive summary", book.description)
    }

    @Test
    fun aLibriVoxMatchSuppliesTitlesAndDescriptionAndKeepsTheArchiveSizes() {
        val item = item(files = listOf(file("wonderland_ch_01_64kb.mp3", track = "1/1")))
        val match = LibriVoxFeedBook(
            id = "200",
            urlIArchive = "https://www.archive.org/details/alice",
            description = "LibriVox <b>summary</b>",
            sections = listOf(
                LibriVoxFeedSection("Down the Rabbit-Hole", "https://archive.org/download/alice/wonderland_ch_01_64kb.mp3", "0")
            ),
        )

        val book = ArchiveCatalog.book(item, match)!!
        assertEquals("Down the Rabbit-Hole", book.sections.single().title)
        assertEquals(640_000L, book.sections.single().durationMs)
        assertEquals(1_000L, book.sections.single().sizeBytes)
        assertEquals("LibriVox summary", book.description)
        assertEquals("alice", match.archiveIdentifier)
    }

    @Test
    fun anItemWithoutChapterFilesIsNoBook() {
        assertNull(ArchiveCatalog.book(item(files = emptyList()), match = null))
    }

    @Test
    fun htmlIsStrippedWithLineBreaksKept() {
        assertEquals("A \"tale\"\nof &lt; two", CatalogText.stripHtml("<p>A &quot;tale&quot;<br/>of &amp;lt; two</p>"))
    }

    @Test
    fun entitiesInTitlesAreDecoded() {
        assertEquals("El celoso extremeño", CatalogText.decodeEntities("El celoso extreme&ntilde;o"))
        assertEquals("Às Ávores, ça & ü", CatalogText.decodeEntities("&Agrave;s &Aacute;vores, &ccedil;a &amp; &uuml;"))
        assertEquals("¿Qué? — «sí»", CatalogText.decodeEntities("&iquest;Qu&#233;? &mdash; &laquo;s&#xED;&raquo;"))
        assertEquals("&lt; stays, &bogus; too", CatalogText.decodeEntities("&amp;lt; stays, &bogus; too"))
    }

    @Test
    fun chapterTitlesFromTheFeedAreDecoded() {
        val sections = ArchiveCatalog.sections(
            listOf(LibriVoxFeedSection("01 - El casamiento enga&ntilde;oso", "https://archive.org/download/x/a.mp3", "60")),
            lengths = emptyMap(),
            sizes = emptyMap(),
        )
        assertEquals("01 - El casamiento engañoso", sections?.single()?.title)
    }

    private fun file(name: String, track: String?, format: String = ArchiveCatalog.CHAPTER_FORMAT, title: String? = null) =
        ArchiveFile(name = name, format = format, size = "1000", length = "10:40", title = title, track = track)

    private fun item(files: List<ArchiveFile>) = ArchiveItem(
        identifier = "alice",
        title = "Alice, by Lewis Carroll",
        creators = listOf("Lewis Carroll"),
        languages = listOf("eng"),
        description = "<p>Archive summary</p>",
        mediatype = "audio",
        runtime = "2:58:51",
        files = files,
    )
}
