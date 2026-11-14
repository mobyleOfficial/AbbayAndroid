package com.mobyle.abbay.presentation.playback.auto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.playback.command.BookNavigator
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.model.Book
import com.model.BookFile
import com.model.BookState
import com.model.MultipleBooks
import com.usecase.GetLastBook
import com.usecase.ObserveBookmarks
import com.usecase.ObserveBooks
import kotlinx.coroutines.flow.first

@OptIn(UnstableApi::class)
@Suppress("TooManyFunctions")
class AutoLibrary(
    private val context: Context,
    private val observeBooks: ObserveBooks,
    private val observeBookmarks: ObserveBookmarks,
    private val getLastBook: GetLastBook,
) {
    fun root(): MediaItem = folder(ROOT, context.getString(R.string.auto_root))

    suspend fun children(parentId: String, currentBookId: String?, browserPackage: String): List<MediaItem> {
        val books = observeBooks().first().filterNot { it.hasError }
        return when (parentId) {
            ROOT -> listOf(
                folder(CONTINUE, context.getString(R.string.auto_continue_listening)),
                folder(IN_PROGRESS, context.getString(R.string.auto_in_progress)),
                folder(NEW, context.getString(R.string.auto_new)),
                folder(FINISHED, context.getString(R.string.auto_finished)),
                folder(BOOKMARKS, context.getString(R.string.auto_bookmarks)),
            )

            CONTINUE -> books.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }.take(CONTINUE_COUNT)
                .map { bookItem(it, browserPackage) }
            IN_PROGRESS -> books.filter { it.state == BookState.IN_PROGRESS }.map { bookItem(it, browserPackage) }
            NEW -> books.filter { it.state == BookState.NEW }.map { bookItem(it, browserPackage) }
            FINISHED -> books.filter { it.state == BookState.FINISHED }.map { bookItem(it, browserPackage) }
            BOOKMARKS -> bookmarkItems(books.firstOrNull { it.id == currentBookId }, browserPackage)
            else -> chapterItems(books.firstOrNull { it.id == bookIdOf(parentId) }, browserPackage)
        }
    }

    suspend fun search(query: String, browserPackage: String): List<MediaItem> =
        BookSearch.search(observeBooks().first().filterNot { it.hasError }, query).map { bookItem(it, browserPackage) }

    @Suppress("ReturnCount")
    suspend fun resolve(requested: MediaItem): MediaSession.MediaItemsWithStartPosition? {
        val books = observeBooks().first().filterNot { it.hasError }
        val query = requested.requestMetadata.searchQuery
        val mediaId = requested.mediaId
        val book = when {
            query != null -> BookSearch.search(books, query).firstOrNull() ?: getLastBook()
            mediaId.isEmpty() -> getLastBook()
            else -> books.firstOrNull { it.id == bookIdOf(mediaId) }
        } ?: return null
        val (items, savedTrack) = BookMediaItems.of(book)
        val target = when {
            mediaId.startsWith(CHAPTER_PREFIX) -> book.chapters.getOrNull(mediaId.substringAfterLast('/').toIntOrNull() ?: -1)?.startMs
            mediaId.startsWith(BOOKMARK_PREFIX) -> mediaId.substringAfterLast('/').toLongOrNull()
            else -> null
        } ?: return MediaSession.MediaItemsWithStartPosition(items, savedTrack, book.progress)
        val position = BookNavigator(trackDurationsOf(book)).positionAt(target)
        return MediaSession.MediaItemsWithStartPosition(items, position.trackIndex, position.positionMs)
    }

    private fun bookItem(book: Book, browserPackage: String): MediaItem = MediaItem.Builder()
        .setMediaId(BOOK_PREFIX + book.id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(book.name)
                .setArtist(book.author)
                .setArtworkUri(artwork(book.thumbnail, browserPackage))
                .setIsPlayable(true)
                .setIsBrowsable(book.chapters.size > 1)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
                .build(),
        )
        .build()

    private fun chapterItems(book: Book?, browserPackage: String): List<MediaItem> = book?.chapters.orEmpty().map { chapter ->
        val title = chapter.title.ifBlank { context.getString(R.string.auto_chapter_number, chapter.index + 1) }
        MediaItem.Builder()
            .setMediaId("$CHAPTER_PREFIX${book?.id}/${chapter.index}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(book?.name)
                    .setArtworkUri(artwork(book?.thumbnail, browserPackage))
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                    .build(),
            )
            .build()
    }

    private suspend fun bookmarkItems(book: Book?, browserPackage: String): List<MediaItem> {
        book ?: return emptyList()
        return observeBookmarks(book.id).first().map { bookmark ->
            val time = formatPosition(bookmark.bookPositionMs)
            MediaItem.Builder()
                .setMediaId("$BOOKMARK_PREFIX${book.id}/${bookmark.bookPositionMs}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(bookmark.note.ifBlank { bookmark.chapterTitle.ifBlank { time } })
                        .setSubtitle(time)
                        .setArtworkUri(artwork(book.thumbnail, browserPackage))
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .build(),
                )
                .build()
        }
    }

    private fun folder(id: String, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
                .setExtras(Bundle())
                .build(),
        )
        .build()

    private fun artwork(thumbnail: String?, browserPackage: String): Uri? =
        thumbnail?.let { CoverProvider.uriFor(context, it, browserPackage) }

    private fun trackDurationsOf(book: Book): List<Long> = when (book) {
        is MultipleBooks -> book.bookFileList.map { it.duration }
        is BookFile -> listOf(book.duration)
        else -> emptyList()
    }

    private fun bookIdOf(mediaId: String): String? = when {
        mediaId.startsWith(BOOK_PREFIX) -> mediaId.removePrefix(BOOK_PREFIX)
        mediaId.startsWith(CHAPTER_PREFIX) -> mediaId.removePrefix(CHAPTER_PREFIX).substringBefore('/')
        mediaId.startsWith(BOOKMARK_PREFIX) -> mediaId.removePrefix(BOOKMARK_PREFIX).substringBefore('/')
        else -> null
    }

    private fun formatPosition(milliseconds: Long): String {
        val totalSeconds = milliseconds / MILLISECONDS_PER_SECOND
        return "%d:%02d:%02d".format(
            totalSeconds / SECONDS_PER_HOUR,
            totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
            totalSeconds % SECONDS_PER_MINUTE
        )
    }

    companion object {
        const val ROOT = "root"
        private const val CONTINUE = "continue"
        private const val IN_PROGRESS = "inProgress"
        private const val NEW = "new"
        private const val FINISHED = "finished"
        private const val BOOKMARKS = "bookmarks"
        private const val BOOK_PREFIX = "book/"
        private const val CHAPTER_PREFIX = "chapter/"
        private const val BOOKMARK_PREFIX = "bookmark/"
        private const val CONTINUE_COUNT = 10
        private const val MILLISECONDS_PER_SECOND = 1_000L
        private const val SECONDS_PER_MINUTE = 60L
        private const val SECONDS_PER_HOUR = 3_600L
    }
}
