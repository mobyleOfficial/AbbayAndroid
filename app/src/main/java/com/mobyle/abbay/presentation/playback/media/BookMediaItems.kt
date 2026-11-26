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

        val items = tracks.mapIndexed { index, track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .withBookInfo(book, trackTitle(book, track.fileName, tracks.size))
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

    /**
     * [item] with [book]'s current title, author and cover, or null when it already shows them. Only the displayed
     * metadata changes, so the player can swap it in without reloading the track.
     */
    fun withBookInfo(item: MediaItem, book: Book): MediaItem? {
        val metadata = item.mediaMetadata
        val trackCount = metadata.totalTrackCount ?: 1
        val updated = metadata.buildUpon().withBookInfo(book, trackTitle(book, fileNameOf(item), trackCount)).build()
        return if (updated == metadata) null else item.buildUpon().setMediaMetadata(updated).build()
    }

    private fun MediaMetadata.Builder.withBookInfo(book: Book, trackTitle: String): MediaMetadata.Builder =
        setTitle(trackTitle)
            .setArtist(listOfNotNull(book.name, book.author?.takeIf(String::isNotBlank)).joinToString(" — "))
            .setAlbumTitle(book.name)
            .setAlbumArtist(book.author)
            .setArtworkUri(book.thumbnail?.let(Uri::parse))

    private fun trackTitle(book: Book, fileName: String, trackCount: Int): String =
        if (trackCount == 1) book.name else fileName.substringBeforeLast('.')

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
