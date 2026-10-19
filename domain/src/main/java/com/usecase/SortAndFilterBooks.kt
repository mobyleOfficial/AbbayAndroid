package com.usecase

import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.library.NaturalOrderComparator
import com.model.Book
import com.model.BookState
import javax.inject.Inject

/** Applies the library tab and sort order. Pure function. */
class SortAndFilterBooks @Inject constructor() {
    private val naturalOrder = NaturalOrderComparator()

    operator fun invoke(books: List<Book>, tab: LibraryTab, sortOrder: LibrarySortOrder, ascending: Boolean): List<Book> {
        val visibleBooks = when (tab) {
            LibraryTab.ALL -> books
            LibraryTab.IN_PROGRESS -> books.filter { it.state == BookState.IN_PROGRESS }
            LibraryTab.NEW -> books.filter { it.state == BookState.NEW }
            LibraryTab.FINISHED -> books.filter { it.state == BookState.FINISHED }
        }
        val comparator: Comparator<Book> = when (sortOrder) {
            LibrarySortOrder.TITLE -> compareBy(naturalOrder) { it.name }
            LibrarySortOrder.AUTHOR -> compareBy<Book, String>(naturalOrder) { it.author.orEmpty() }
                .thenBy(naturalOrder) { it.name }
            LibrarySortOrder.RECENTLY_PLAYED -> compareBy<Book> { it.lastPlayedAt ?: 0L }.thenBy(naturalOrder) { it.name }
            LibrarySortOrder.RECENTLY_ADDED -> compareBy<Book> { it.addedAt }.thenBy(naturalOrder) { it.name }
            LibrarySortOrder.PROGRESS -> compareBy<Book> { progressFraction(it) }.thenBy(naturalOrder) { it.name }
            LibrarySortOrder.DURATION -> compareBy<Book> { it.duration }.thenBy(naturalOrder) { it.name }
        }

        return visibleBooks.sortedWith(if (ascending) comparator else comparator.reversed())
    }

    /** The tab chosen automatically: In progress when it is non-empty, otherwise All. */
    fun defaultTab(books: List<Book>): LibraryTab =
        if (books.any { it.state == BookState.IN_PROGRESS }) LibraryTab.IN_PROGRESS else LibraryTab.ALL

    private fun progressFraction(book: Book): Double =
        if (book.duration <= 0L) 0.0 else book.bookPositionMs.toDouble() / book.duration
}
