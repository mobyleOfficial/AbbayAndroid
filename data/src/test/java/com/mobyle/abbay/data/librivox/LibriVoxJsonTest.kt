package com.mobyle.abbay.data.librivox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibriVoxJsonTest {
    @Test
    fun searchDocsAcceptAStringOrAnArrayOfCreators() {
        val body = """
            {"response":{"docs":[
              {"identifier":"alice","title":"Alice","creator":"Lewis Carroll","language":"eng","mediatype":"audio"},
              {"identifier":"poems","title":"Poems","creator":["Various", " "],"language":["eng","fre"]}
            ]}}
        """.trimIndent()

        val docs = LibriVoxJson.searchDocs(body)
        assertEquals(listOf("Lewis Carroll"), docs[0].creators)
        assertEquals(listOf("Various"), docs[1].creators)
        assertEquals(listOf("eng", "fre"), docs[1].languages)
        assertNull(docs[1].mediatype)
    }

    @Test
    fun anUnknownArchiveItemIsEmpty() {
        assertNull(LibriVoxJson.archiveItem("missing", "{}"))
        assertEquals(emptyList<ArchiveSearchDoc>(), LibriVoxJson.searchDocs("not json"))
    }

    @Test
    fun archiveFilesReadNumbersSentAsStrings() {
        val body = """{"metadata":{"title":"Alice","creator":"Lewis Carroll","description":["<p>One</p>"]},
            "files":[{"name":"a_64kb.mp3","format":"64Kbps MP3","size":"5124992","length":"10:40","track":"1/12"}]}"""

        val item = LibriVoxJson.archiveItem("alice", body)!!
        assertEquals("<p>One</p>", item.description)
        assertEquals("5124992", item.files.single().size)
        assertEquals("Lewis Carroll", item.creator)
    }

    @Test
    fun feedBooksReadSections() {
        val body = """{"books":[{"id":"200","url_iarchive":"https:\/\/www.archive.org\/details\/alice_librivox\/",
            "sections":[{"title":"Chapter 1","listen_url":"https://archive.org/download/alice/1.mp3","playtime":"640"}]}]}"""

        val book = LibriVoxJson.feedBooks(body).single()
        assertEquals("alice_librivox", book.archiveIdentifier)
        assertEquals("640", book.sections.single().playtime)
        assertEquals(emptyList<LibriVoxFeedBook>(), LibriVoxJson.feedBooks("""{"error":"Audiobooks could not be found"}"""))
    }
}
