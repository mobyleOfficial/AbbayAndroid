package com.mobyle.abbay.data.librivox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class LibriVoxHttpTest {
    @Test
    fun onlyHttpsArchiveAndLibriVoxHostsAreAllowed() {
        assertTrue(LibriVoxHttp.isAllowed(URL("https://archive.org/metadata/alice")))
        assertTrue(LibriVoxHttp.isAllowed(URL("https://ia800300.us.archive.org/1/items/alice/a.mp3")))
        assertTrue(LibriVoxHttp.isAllowed(URL("https://librivox.org/api/feed/audiobooks/")))
        assertFalse(LibriVoxHttp.isAllowed(URL("http://archive.org/metadata/alice")))
        assertFalse(LibriVoxHttp.isAllowed(URL("https://notarchive.org/a")))
        assertFalse(LibriVoxHttp.isAllowed(URL("https://archive.org.example.com/a")))
    }
}
