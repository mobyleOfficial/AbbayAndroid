package com.usecase

import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.model.BookFile
import com.model.BookState
import com.model.BookType
import org.junit.Assert.assertEquals
import org.junit.Test

class SortAndFilterBooksTest {
    private val sortAndFilter = SortAndFilterBooks()

    private fun book(
        name: String,
        state: BookState = BookState.NEW,
        author: String? = null,
        addedAt: Long = 0L,
        lastPlayedAt: Long? = null,
        progress: Long = 0L,
        duration: Long = 1_000L,
    ) = BookFile(
        id = name,
        name = name,
        thumbnail = null,
        progress = progress,
        duration = duration,
        speed = 1f,
        type = BookType.FOLDER,
        fileName = name,
        author = author,
        state = state,
        addedAt = addedAt,
        lastPlayedAt = lastPlayedAt,
    )

    private val books = listOf(
        book("Book 10", BookState.IN_PROGRESS, author = "Zed", addedAt = 3, lastPlayedAt = 30, progress = 900),
        book("Book 2", BookState.FINISHED, author = "Amy", addedAt = 1, lastPlayedAt = 10, progress = 1_000),
        book("Book 1", BookState.NEW, author = "Amy", addedAt = 2, duration = 5_000),
    )

    private fun names(tab: LibraryTab, order: LibrarySortOrder, ascending: Boolean = true) =
        sortAndFilter(books, tab, order, ascending).map { it.name }

    @Test
    fun `tabs filter by book state`() {
        assertEquals(listOf("Book 10"), names(LibraryTab.IN_PROGRESS, LibrarySortOrder.TITLE))
        assertEquals(listOf("Book 1"), names(LibraryTab.NEW, LibrarySortOrder.TITLE))
        assertEquals(listOf("Book 2"), names(LibraryTab.FINISHED, LibrarySortOrder.TITLE))
    }

    @Test
    fun `title sort is natural and can be reversed`() {
        assertEquals(listOf("Book 1", "Book 2", "Book 10"), names(LibraryTab.ALL, LibrarySortOrder.TITLE))
        assertEquals(listOf("Book 10", "Book 2", "Book 1"), names(LibraryTab.ALL, LibrarySortOrder.TITLE, ascending = false))
    }

    @Test
    fun `author sort breaks ties by title`() {
        assertEquals(listOf("Book 1", "Book 2", "Book 10"), names(LibraryTab.ALL, LibrarySortOrder.AUTHOR))
    }

    @Test
    fun `recently played descending puts the last played book first and never played last`() {
        assertEquals(listOf("Book 10", "Book 2", "Book 1"), names(LibraryTab.ALL, LibrarySortOrder.RECENTLY_PLAYED, ascending = false))
    }

    @Test
    fun `recently added, progress and duration`() {
        assertEquals(listOf("Book 2", "Book 1", "Book 10"), names(LibraryTab.ALL, LibrarySortOrder.RECENTLY_ADDED))
        assertEquals(listOf("Book 1", "Book 10", "Book 2"), names(LibraryTab.ALL, LibrarySortOrder.PROGRESS))
        assertEquals(listOf("Book 2", "Book 10", "Book 1"), names(LibraryTab.ALL, LibrarySortOrder.DURATION))
    }

    @Test
    fun `default tab is In progress only when it has books`() {
        assertEquals(LibraryTab.IN_PROGRESS, sortAndFilter.defaultTab(books))
        assertEquals(LibraryTab.ALL, sortAndFilter.defaultTab(books.filter { it.state != BookState.IN_PROGRESS }))
    }
}
