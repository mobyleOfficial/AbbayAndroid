package com.mobyle.abbay.data.library

import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.model.Book
import com.model.BookFile
import com.model.BookState
import com.model.BookType
import com.model.Chapter
import com.model.MultipleBooks

/**
 * Maps the library tables to the read model of the preserved UI: one playable track → [BookFile],
 * several → [MultipleBooks]. The track index of the listening state counts playable tracks only.
 */
internal object BookProjection {
    fun toBook(details: BookWithDetails, isRootAccessible: Boolean, defaultSpeed: Float = 1f): Book? {
        val book = details.book
        val tracks = playableTracks(details)
        if (tracks.isEmpty()) return null

        val listeningState = details.listeningState
        val trackIndex = (listeningState?.trackIndex ?: 0).coerceIn(0, tracks.lastIndex)
        val state = stateOf(listeningState?.state)
        val type = if (book.rootId == null || book.isSingleFile) BookType.FILE else BookType.FOLDER
        val hasError = book.missingSince != null || !isRootAccessible
        val speed = listeningState?.speed ?: defaultSpeed
        val positionInTrack = listeningState?.positionInTrackMs ?: 0L
        val chapters = chaptersOf(details, tracks)

        return if (tracks.size == 1) {
            val track = tracks.single()
            BookFile(
                id = book.id.toString(),
                name = book.displayTitle,
                thumbnail = book.coverUri,
                progress = positionInTrack,
                duration = track.durationMs,
                speed = speed,
                hasError = hasError,
                type = type,
                fileName = track.fileName,
                uri = track.documentUri,
                author = book.displayAuthor,
                state = state,
                addedAt = book.addedAt,
                lastPlayedAt = listeningState?.lastPlayedAt,
                chapters = chapters,
                lastPausedAt = listeningState?.lastPausedAt,
                narrator = book.displayNarrator,
                rootId = book.rootId,
                relativePath = book.relativePath,
            )
        } else {
            MultipleBooks(
                id = book.id.toString(),
                name = book.displayTitle,
                thumbnail = book.coverUri,
                progress = positionInTrack,
                duration = tracks.sumOf { it.durationMs },
                speed = speed,
                hasError = hasError,
                type = type,
                bookFileList = tracks.map { track -> trackAsBookFile(details, track, speed, hasError, type) },
                currentBookPosition = trackIndex,
                author = book.displayAuthor,
                state = state,
                addedAt = book.addedAt,
                lastPlayedAt = listeningState?.lastPlayedAt,
                chapters = chapters,
                lastPausedAt = listeningState?.lastPausedAt,
                narrator = book.displayNarrator,
                rootId = book.rootId,
                relativePath = book.relativePath,
            )
        }
    }

    /** Title of the chapter at [bookPositionMs]; blank when the chapter has none. */
    fun chapterTitleAt(details: BookWithDetails, bookPositionMs: Long): String {
        val chapters = chaptersOf(details, playableTracks(details))
        return (chapters.lastOrNull { it.startMs <= bookPositionMs } ?: chapters.firstOrNull())?.title.orEmpty()
    }

    private fun stateOf(name: String?): BookState = BookState.entries.firstOrNull { it.name == name } ?: BookState.NEW

    /**
     * Stored chapters, or one chapter per track when there are none or when the book uses track-number order (stored
     * chapter times follow the natural order).
     */
    private fun chaptersOf(details: BookWithDetails, tracks: List<TrackEntity>): List<Chapter> {
        val stored = details.chapters.sortedBy { it.indexInBook }
        if (stored.isNotEmpty() && details.listeningState?.sortByTrackTag != true) {
            return stored.mapIndexed { index, chapter -> Chapter(index, chapter.title, chapter.startMs, chapter.endMs) }
        }
        var start = 0L
        return tracks.mapIndexed { index, track ->
            val title = track.titleTag?.takeIf(String::isNotBlank) ?: track.fileName.substringBeforeLast('.')
            Chapter(index, title, start, start + track.durationMs).also { start += track.durationMs }
        }
    }

    private fun trackAsBookFile(details: BookWithDetails, track: TrackEntity, speed: Float, hasError: Boolean, type: BookType) =
        BookFile(
            id = "${details.book.id}/${track.id}",
            name = details.book.displayTitle,
            thumbnail = details.book.coverUri,
            progress = 0L,
            duration = track.durationMs,
            speed = speed,
            hasError = hasError,
            type = type,
            fileName = track.fileName,
            uri = track.documentUri,
        )

    /** Natural order, or track-number tags when the book asks for it. */
    fun playableTracks(details: BookWithDetails): List<TrackEntity> {
        val playable = details.tracks.filterNot { it.unplayable }.sortedBy { it.indexInBook }
        return if (details.listeningState?.sortByTrackTag == true) {
            playable.sortedWith(compareBy<TrackEntity> { it.trackNumberTag ?: Int.MAX_VALUE }.thenBy { it.indexInBook })
        } else {
            playable
        }
    }
}

/** User overrides win over scanned tags. */
internal val BookEntity.displayTitle: String get() = titleOverride ?: title
internal val BookEntity.displayAuthor: String? get() = authorOverride ?: author

internal val BookEntity.displayNarrator: String? get() = narratorOverride ?: narrator
