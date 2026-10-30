package com.library

import com.model.BookFile
import com.model.BookType
import com.model.LibraryRoot
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderTreeTest {
    private val roots = listOf(LibraryRoot(1, "tree-1", "Audiobooks", true), LibraryRoot(2, "tree-2", "Empty", true))
    private val books = listOf(
        book("a", 1, "Dune"),
        book("b", 1, "Series/Book 2"),
        book("c", 1, "Series/Book 10"),
        book("d", 1, "Series/Extras/Interview"),
        book("e", null, "loose.m4b"),
    )

    @Test
    fun topLevelListsRootsWithBooksAndTheSingleFiles() {
        val top = FolderTree.level(roots, books, FolderLocation.TOP)
        assertEquals(listOf(FolderTree.SINGLE_FILES, "Audiobooks"), top.folders.map { it.name })
        assertEquals(listOf(1, 4), top.folders.map { it.bookCount })
    }

    @Test
    fun aFolderShowsItsSubFoldersAndItsBooksInNaturalOrder() {
        val root = FolderTree.level(roots, books, FolderLocation(rootId = 1))
        assertEquals(listOf("Series"), root.folders.map { it.name })
        assertEquals(3, root.folders.single().bookCount)
        assertEquals(listOf("a"), root.books.map { it.id })

        val series = FolderTree.level(roots, books, FolderLocation(rootId = 1).child("Series"))
        assertEquals(listOf("Extras"), series.folders.map { it.name })
        assertEquals(listOf("b", "c"), series.books.map { it.id })
        assertEquals(FolderLocation(1, "Series"), FolderTree.locationOf(books[1]))
        assertEquals(FolderLocation(1, ""), FolderLocation(1, "Series/Extras").upTo(0))
    }

    @Test
    fun singleFilesHaveTheirOwnGroup() {
        val singles = FolderTree.level(roots, books, FolderLocation(isSingleFiles = true))
        assertEquals(listOf("e"), singles.books.map { it.id })
        assertEquals(FolderLocation(isSingleFiles = true), FolderTree.locationOf(books.last()))
    }

    private fun book(id: String, rootId: Long?, relativePath: String) = BookFile(
        id = id,
        name = relativePath.substringAfterLast('/'),
        thumbnail = null,
        progress = 0,
        duration = 1,
        speed = 1f,
        type = BookType.FOLDER,
        fileName = "$id.mp3",
        rootId = rootId,
        relativePath = relativePath,
    )
}
