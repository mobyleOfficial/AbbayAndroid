package com.readalong

import com.model.BookFile
import com.model.BookType
import com.model.MultipleBooks
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAlongAvailabilityTest {
    private fun file(uri: String) = BookFile(
        id = uri,
        name = "chapter",
        thumbnail = null,
        progress = 0,
        duration = 1_000,
        speed = 1f,
        type = BookType.FILE,
        fileName = "a.mp3",
        uri = uri,
    )

    private fun book(vararg uris: String) = MultipleBooks(
        id = "book",
        name = "Book",
        thumbnail = null,
        progress = 0,
        duration = 1_000,
        speed = 1f,
        type = BookType.FOLDER,
        bookFileList = uris.map(::file),
    )

    @Test
    fun `a book only streamed has nothing to read`() {
        assertFalse(ReadAlongAvailability.isOffered(book("https://archive.org/a.mp3", "https://archive.org/b.mp3")))
    }

    @Test
    fun `one chapter downloaded is enough`() {
        assertTrue(ReadAlongAvailability.isOffered(book("https://archive.org/a.mp3", "file:///data/librivox/b.mp3")))
    }

    @Test
    fun `a book from the device's folders has its audio here`() {
        assertTrue(ReadAlongAvailability.isOffered(file("content://com.android.externalstorage.documents/tree/a")))
    }
}
