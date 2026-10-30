package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerPosition
import com.mobyle.abbay.presentation.booklist.widgets.player.progressFraction
import com.mobyle.abbay.presentation.booklist.widgets.player.timeLeftLabel
import com.model.Book
import com.model.BookState
import com.model.LibriVoxDownload
import com.model.LibriVoxDownloadMode

val LocalLibriVoxDownloads = compositionLocalOf<Map<String, LibriVoxDownload>> { emptyMap() }

@Composable
fun bookStatusLine(book: Book): String {
    val download = LocalLibriVoxDownloads.current[book.id]
    return when (download?.mode) {
        LibriVoxDownloadMode.PAUSED -> stringResource(R.string.librivox_book_status_paused)
        LibriVoxDownloadMode.ACTIVE -> stringResource(R.string.librivox_book_status_downloading, (download.fraction * PERCENT).toInt())
        LibriVoxDownloadMode.OFF, null -> listeningStatusLine(book)
    }
}

@Composable
fun isStreamingOnly(book: Book): Boolean = LocalLibriVoxDownloads.current[book.id]?.mode == LibriVoxDownloadMode.OFF

@Composable
private fun listeningStatusLine(book: Book): String {
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

fun bookStartedFraction(book: Book): Float? = when {
    book.state == BookState.FINISHED || book.bookPositionMs <= 0L || book.duration <= 0L -> null
    else -> progressFraction(book.bookPositionMs, book.duration)
}

private const val PERCENT = 100
