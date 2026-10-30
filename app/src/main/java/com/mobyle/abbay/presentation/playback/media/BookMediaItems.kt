package com.mobyle.abbay.presentation.playback.media

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mobyle.abbay.presentation.playback.listening.PauseRecord
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks

object BookMediaItems {
    private const val EXTRA_BOOK_ID = "bookId"
    private const val EXTRA_TRACK_DURATION_MS = "trackDurationMs"
    private const val EXTRA_FILE_NAME = "fileName"
    private const val EXTRA_CHAPTER_STARTS_MS = "chapterStartsMs"
    private const val EXTRA_LAST_PAUSED_AT = "lastPausedAt"
    private const val EXTRA_CHAPTER_TITLES = "chapterTitles"
    private const val EXTRA_SAVED_BOOK_POSITION_MS = "savedBookPositionMs"

    fun of(book: Book): Pair<List<MediaItem>, Int> {
        val (tracks, startIndex) = when (book) {
            is MultipleBooks -> book.bookFileList to book.currentBookPosition
            is BookFile -> listOf(book) to 0
            else -> return emptyList<MediaItem>() to 0
        }
        val chapterStarts = book.chapters.map { it.startMs }.toLongArray()
        val chapterTitles = book.chapters.map { it.title }.toTypedArray()
        val subtitle = listOfNotNull(book.name, book.author?.takeIf(String::isNotBlank)).joinToString(" — ")

        val items = tracks.mapIndexed { index, track ->
            val trackTitle = if (tracks.size == 1) book.name else track.fileName.substringBeforeLast('.')
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(trackTitle)
                        .setArtist(subtitle)
                        .setAlbumTitle(book.name)
                        .setAlbumArtist(book.author)
                        .setArtworkUri(book.thumbnail?.let(Uri::parse))
                        .setTrackNumber(index + 1)
                        .setTotalTrackCount(tracks.size)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setExtras(
                            Bundle().apply {
                                putString(EXTRA_BOOK_ID, book.id)
                                putLong(EXTRA_TRACK_DURATION_MS, track.duration)
                                putString(EXTRA_FILE_NAME, track.fileName)
                                putLongArray(EXTRA_CHAPTER_STARTS_MS, chapterStarts)
                                putStringArray(EXTRA_CHAPTER_TITLES, chapterTitles)
                                book.lastPausedAt?.let { putLong(EXTRA_LAST_PAUSED_AT, it) }
                                putLong(EXTRA_SAVED_BOOK_POSITION_MS, book.bookPositionMs)
                            },
                        )
                        .build(),
                )
                .build()
        }
        return items to startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    fun bookIdOf(item: MediaItem?): String? = item?.mediaMetadata?.extras?.getString(EXTRA_BOOK_ID)

    fun trackDurationOf(item: MediaItem): Long = item.mediaMetadata.extras?.getLong(EXTRA_TRACK_DURATION_MS) ?: 0L

    fun chapterStartsOf(item: MediaItem): List<Long> = item.mediaMetadata.extras?.getLongArray(EXTRA_CHAPTER_STARTS_MS)?.toList().orEmpty()

    fun storedPauseOf(item: MediaItem?): PauseRecord? {
        val extras = item?.mediaMetadata?.extras ?: return null
        val bookId = extras.getString(EXTRA_BOOK_ID) ?: return null
        if (!extras.containsKey(EXTRA_LAST_PAUSED_AT)) return null
        return PauseRecord(bookId, extras.getLong(EXTRA_LAST_PAUSED_AT), extras.getLong(EXTRA_SAVED_BOOK_POSITION_MS))
    }

    fun chapterTitlesOf(item: MediaItem?): List<String> = item?.mediaMetadata?.extras?.getStringArray(
        EXTRA_CHAPTER_TITLES
    )?.toList().orEmpty()

    fun fileNameOf(item: MediaItem?): String = item?.mediaMetadata?.extras?.getString(EXTRA_FILE_NAME).orEmpty()
}
