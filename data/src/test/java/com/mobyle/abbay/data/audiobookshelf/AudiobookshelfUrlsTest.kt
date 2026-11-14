package com.mobyle.abbay.data.audiobookshelf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class AudiobookshelfUrlsTest {
    @Test
    fun anAddressWithoutSchemeTriesHttpsThenHttp() {
        assertEquals(
            listOf("https://192.168.0.10:13378", "http://192.168.0.10:13378"),
            AudiobookshelfUrls.candidates(" 192.168.0.10:13378/ "),
        )
    }

    @Test
    fun anAddressKeepsItsSchemeAndPath() {
        assertEquals(listOf("https://example.com/abs"), AudiobookshelfUrls.candidates("HTTPS://example.com/abs/"))
    }

    @Test
    fun whatIsNotAWebAddressHasNoCandidates() {
        assertEquals(emptyList<String>(), AudiobookshelfUrls.candidates(""))
        assertEquals(emptyList<String>(), AudiobookshelfUrls.candidates("ftp://example.com"))
        assertEquals(emptyList<String>(), AudiobookshelfUrls.candidates("my server"))
    }

    @Test
    fun libraryIdsAreToldApartFromLibriVoxOnes() {
        val id = AudiobookshelfUrls.libraryId("li_42")

        assertTrue(AudiobookshelfUrls.isAudiobookshelf(id))
        assertEquals("li_42", AudiobookshelfUrls.itemIdOf(id))
        assertFalse(AudiobookshelfUrls.isAudiobookshelf("alice_in_wonderland_librivox"))
        assertNull(AudiobookshelfUrls.itemIdOf("alice_in_wonderland_librivox"))
    }

    @Test
    fun theTokenGoesOnlyToTheSignedInServer() {
        val server = "http://home.lan:13378/abs"

        assertTrue(AudiobookshelfSession.isSameServer(server, URL("http://home.lan:13378/abs/api/items/a/file/1")))
        assertFalse(AudiobookshelfSession.isSameServer(server, URL("http://home.lan:8080/abs/api/items")))
        assertFalse(AudiobookshelfSession.isSameServer(server, URL("http://home.lan:13378/other")))
        assertFalse(AudiobookshelfSession.isSameServer(server, URL("http://evil.example.com:13378/abs/api")))
        assertFalse(AudiobookshelfSession.isSameServer(server, URL("https://archive.org/download/x")))
    }
}
