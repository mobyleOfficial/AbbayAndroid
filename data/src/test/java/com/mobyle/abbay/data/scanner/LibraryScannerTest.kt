package com.mobyle.abbay.data.scanner

import com.mobyle.abbay.data.scanner.InMemoryDocumentTree.Companion.TREE_URI
import com.model.DataResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScannerTest {
    private fun scanAll(tree: InMemoryDocumentTree): List<ScannedBook> {
        val scanner = LibraryScanner(tree)
        val rootChildren = (scanner.listRoot(TREE_URI) as DataResult.Success).value
        return scanner.looseFileBooks(rootChildren) +
            rootChildren.filter { it.isDirectory }.flatMap { scanner.scanFolder(TREE_URI, it, it.name, null) }
    }

    private fun List<ScannedBook>.paths() = map { it.relativePath }.sorted()

    @Test
    fun `each folder with audio is a book and loose root files are one-file books`() {
        val books = scanAll(InMemoryDocumentTree("Dune/01.mp3", "Dune/02.mp3", "Loose Story.m4b"))

        assertEquals(listOf("Dune", "Loose Story.m4b"), books.paths())
        val looseBook = books.single { it.isSingleFile }
        assertEquals("Loose Story", looseBook.folderName)
        assertEquals(2, books.single { it.relativePath == "Dune" }.tracks.size)
    }

    @Test
    fun `folders without audio are collections and the deepest folder with audio is the book`() {
        val books = scanAll(
            InMemoryDocumentTree("Author/Series/Book 1/a.mp3", "Author/Series/Book 2/a.mp3", "Author/Standalone/a.mp3"),
        )

        assertEquals(listOf("Author/Series/Book 1", "Author/Series/Book 2", "Author/Standalone"), books.paths())
        assertEquals("Series", books.first { it.relativePath == "Author/Series/Book 1" }.parentFolderName)
    }

    @Test
    fun `disc folders are merged into one book in natural order`() {
        val books = scanAll(
            InMemoryDocumentTree("Epic/CD 10/01.mp3", "Epic/CD1/02.mp3", "Epic/CD1/01.mp3", "Epic/cd 2/01.mp3", "Epic/cover.jpg"),
        )

        val epic = books.single()
        assertEquals("Epic", epic.relativePath)
        assertEquals(
            listOf("Epic/CD1/01.mp3", "Epic/CD1/02.mp3", "Epic/cd 2/01.mp3", "Epic/CD 10/01.mp3"),
            epic.tracks.map { it.documentId },
        )
        assertEquals(listOf("cover.jpg"), epic.images.map { it.name })
    }

    @Test
    fun `a folder mixing disc folders and other folders is a collection`() {
        val books = scanAll(InMemoryDocumentTree("Shelf/CD1/a.mp3", "Shelf/Bonus/a.mp3"))

        assertEquals(listOf("Shelf/Bonus", "Shelf/CD1"), books.paths())
    }

    @Test
    fun `tracks use natural order and hidden or empty files are ignored`() {
        val tree = InMemoryDocumentTree("Book/Chapter 10.mp3", "Book/Chapter 2.mp3", "Book/Chapter 1.mp3", "Book/.hidden.mp3")
        tree.addFile("Book/empty.mp3", sizeBytes = 0L)

        val book = scanAll(tree).single()

        assertEquals(listOf("Chapter 1.mp3", "Chapter 2.mp3", "Chapter 10.mp3"), book.tracks.map { it.name })
    }

    @Test
    fun `wma files are listed as unsupported, never as tracks`() {
        val book = scanAll(InMemoryDocumentTree("Book/01.mp3", "Book/02.wma")).single()

        assertEquals(listOf("01.mp3"), book.tracks.map { it.name })
        assertEquals(listOf("02.wma"), book.unsupportedFiles.map { it.name })
    }

    @Test
    fun `accepted extensions are case-insensitive and other files are not tracks`() {
        val book = scanAll(InMemoryDocumentTree("Book/A.MP3", "Book/b.Opus", "Book/notes.pdf", "Book/description.txt")).single()

        assertEquals(listOf("A.MP3", "b.Opus"), book.tracks.map { it.name })
        assertEquals("description.txt", book.descriptionFile?.name)
    }

    @Test
    fun `an unreadable folder is skipped without failing the scan`() {
        val tree = InMemoryDocumentTree("Good/a.mp3", "Bad/a.mp3")
        tree.unreadableFolders += "Bad"

        assertEquals(listOf("Good"), scanAll(tree).paths())
    }

    @Test
    fun `each folder is listed once`() {
        val tree = InMemoryDocumentTree("A/1.mp3", "B/C/1.mp3", "B/D/1.mp3")
        scanAll(tree)

        assertEquals(5, tree.listCalls)
    }

    @Test
    fun `preferred cover names win over larger images`() {
        val tree = InMemoryDocumentTree("Book/a.mp3")
        tree.addFile("Book/big.png", sizeBytes = 9_000_000L)
        tree.addFile("Book/Folder.jpg", sizeBytes = 10L)
        val book = scanAll(tree).single()

        assertEquals("Folder.jpg", FileKinds.preferredCover(book.images)?.name)
        assertTrue(FileKinds.isDiscFolder(tree.entry("Book").copy(name = "Part 3")))
        assertNull(FileKinds.preferredCover(emptyList()))
    }
}
