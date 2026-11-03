package com.mobyle.abbay.data.readalong

import com.readalong.ReadAlongLine
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongTranscript
import com.readalong.ReadAlongWord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReadAlongTranscriptStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val root: File get() = File(folder.root, "readalong")
    private val store by lazy { ReadAlongTranscriptStore(root) }

    private val transcript = ReadAlongTranscript(
        language = "en-US",
        lines = listOf(ReadAlongLine(1_000, 1_900, listOf(ReadAlongWord("hello", 1_000, 1_400), ReadAlongWord("there", 1_450, 1_900)))),
        covered = listOf(ReadAlongSpan(0, 10_000)),
    )

    @Test
    fun `a saved transcript reads back the same`() = runTest {
        val stamp = store.load("book").stamp
        store.save("book", transcript, stamp)

        assertEquals(transcript, store.load("book").transcript)
    }

    @Test
    fun `files are named after a hash, never the book's id`() = runTest {
        store.save("Secret Title", transcript, store.load("Secret Title").stamp)

        val names = root.list().orEmpty().toList()
        assertEquals(1, names.size)
        assertFalse(names.single().contains("Secret"))
        assertTrue(names.single().matches(Regex("[0-9a-f]{64}\\.json")))
    }

    @Test
    fun `a save stamped before a delete is dropped`() = runTest {
        val stamp = store.load("book").stamp
        store.delete("book")
        store.save("book", transcript, stamp)

        assertNull(store.load("book").transcript)
    }

    @Test
    fun `deleting everything removes the folder and stale saves`() = runTest {
        store.save("a", transcript, store.load("a").stamp)
        val stamp = store.load("a").stamp

        store.deleteAll()
        store.save("a", transcript, stamp)

        assertNull(store.load("a").transcript)
    }

    @Test
    fun `an unreadable file is forgotten`() = runTest {
        store.save("book", transcript, store.load("book").stamp)
        root.listFiles()!!.single().writeText("{ not json")

        assertNull(store.load("book").transcript)
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }
}
