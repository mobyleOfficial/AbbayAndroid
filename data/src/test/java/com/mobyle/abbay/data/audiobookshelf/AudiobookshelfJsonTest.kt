package com.mobyle.abbay.data.audiobookshelf

import com.model.AudiobookshelfLibrary
import com.model.CatalogChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookshelfJsonTest {
    private val server = "https://abs.example.com"
    private val coverOf = { id: String -> "$server/api/items/$id/cover" }

    @Test
    fun aRefreshableSessionKeepsBothTokens() {
        val body = """{"user":{"token":"legacy","accessToken":"access","refreshToken":"refresh"}}"""

        assertEquals(AudiobookshelfJson.Tokens("access", "refresh"), AudiobookshelfJson.tokens(body))
    }

    @Test
    fun anOlderServerGivesItsLongLivedToken() {
        assertEquals(AudiobookshelfJson.Tokens("legacy", null), AudiobookshelfJson.tokens("""{"user":{"token":"legacy"}}"""))
        assertNull(AudiobookshelfJson.tokens("""{"error":"nope"}"""))
    }

    @Test
    fun onlyBookLibrariesAreListed() {
        val body = """{"libraries":[
            {"id":"lib1","name":"Audiobooks","mediaType":"book"},
            {"id":"lib2","name":"Podcasts","mediaType":"podcast"}
        ]}"""

        assertEquals(listOf(AudiobookshelfLibrary("lib1", "Audiobooks")), AudiobookshelfJson.libraries(body))
    }

    @Test
    fun pageItemsSkipMissingAndEbookOnlyItems() {
        val body = """{"results":[
            {"id":"a","mediaType":"book","media":{"metadata":{"title":"Dune","authorName":"Frank Herbert"},"duration":3600.5,"numAudioFiles":2}},
            {"id":"b","mediaType":"book","isMissing":true,"media":{"metadata":{"title":"Gone"},"numAudioFiles":1}},
            {"id":"c","mediaType":"book","media":{"metadata":{"title":"Ebook"},"numAudioFiles":0}}
        ]}"""

        val books = AudiobookshelfJson.pageItems(body, coverOf)
        assertEquals(listOf("a"), books.map { it.id })
        assertEquals("Frank Herbert", books.single().authors)
        assertEquals(3_600_500L, books.single().durationMs)
        assertEquals("$server/api/items/a/cover", books.single().coverUrl)
    }

    @Test
    fun searchReadsAuthorsFromTheirNames() {
        val body = """{"book":[{"libraryItem":{"id":"a","mediaType":"book",
            "media":{"metadata":{"title":"Good Omens",
            "authors":[{"id":"1","name":"Terry Pratchett"},{"id":"2","name":"Neil Gaiman"}]}}}}]}"""

        assertEquals("Terry Pratchett, Neil Gaiman", AudiobookshelfJson.searchItems(body, coverOf).single().authors)
    }

    @Test
    fun aBookListsItsPlayableFilesInOrderWithTheirChapters() {
        val body = """{"id":"li_1","mediaType":"book","media":{
            "metadata":{"title":"Dune","authorName":"Frank Herbert","description":"<p>Spice &amp; sand</p>","language":"en"},
            "audioFiles":[
              {"index":2,"ino":"22","duration":20.0,"metadata":{"filename":"02.mp3","size":2000}},
              {"index":1,"ino":"11","duration":10.0,"metadata":{"filename":"01.mp3","size":1000},"metaTags":{"tagTitle":"Opening"}},
              {"index":3,"ino":"33","duration":5.0,"exclude":true,"metadata":{"filename":"extra.mp3","size":10}}
            ],
            "chapters":[{"id":0,"start":0,"end":10,"title":"One"},{"id":1,"start":10,"end":30,"title":"Two"}]
        }}"""

        val book = AudiobookshelfJson.book(body, server, coverOf)!!
        assertEquals(listOf("Opening", "02"), book.sections.map { it.title })
        assertEquals(listOf("01.mp3", "02.mp3"), book.sections.map { it.fileName })
        assertEquals("$server/api/items/li_1/file/11", book.sections.first().listenUrl)
        assertEquals(3_000L, book.totalBytes)
        assertEquals(30_000L, book.durationMs)
        assertEquals("Spice & sand", book.description)
        assertEquals(listOf(CatalogChapter("One", 0L, 10_000L), CatalogChapter("Two", 10_000L, 30_000L)), book.chapters)
    }

    @Test
    fun aBookWithoutAudioIsNotOne() {
        val body = """{"id":"e","mediaType":"book","media":{"metadata":{"title":"Ebook"},"audioFiles":[]}}"""

        assertNull(AudiobookshelfJson.book(body, server, coverOf))
    }

    @Test
    fun theStatusPageTellsAServerApart() {
        assertTrue(AudiobookshelfJson.isServerStatus("""{"isInit":true,"serverVersion":"2.17.0"}"""))
        assertEquals(false, AudiobookshelfJson.isServerStatus("<html></html>"))
        assertTrue(AudiobookshelfJson.isPing("""{"success":true}"""))
    }
}
