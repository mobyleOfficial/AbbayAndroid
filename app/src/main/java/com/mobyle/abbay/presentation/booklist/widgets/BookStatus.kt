package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerPosition
import com.mobyle.abbay.presentation.booklist.widgets.player.progressFraction
import com.mobyle.abbay.presentation.booklist.widgets.player.timeLeftLabel
import com.model.Book
import com.model.BookState

/**
 * A library cell's one status line (AbbayiOS's `MediaView.detailLine`): *Finished ✓*, *New* before it is started,
 * then *Ch N/M* for a book with chapters or the time left for one without.
 */
@Composable
fun bookStatusLine(book: Book): String {
    val position = PlayerPosition.of(book)
    val chapter = position.chapter
    return when {
        book.state == BookState.FINISHED -> stringResource(R.string.book_status_finished)
        book.state == BookState.NEW || book.bookPositionMs <= 0L -> stringResource(R.string.book_status_new)
        chapter != null && position.chapterCount > 1 -> stringResource(
            R.string.book_status_chapter,
            chapter.index + 1,
            position.chapterCount
        )
        else -> timeLeftLabel(book.duration - book.bookPositionMs, book.speed)
    }
}

/** 0…1 through the book while it is started and not finished; null otherwise (the cell shows no bar). */
fun bookStartedFraction(book: Book): Float? = when {
    book.state == BookState.FINISHED || book.bookPositionMs <= 0L || book.duration <= 0L -> null
    else -> progressFraction(book.bookPositionMs, book.duration)
}
