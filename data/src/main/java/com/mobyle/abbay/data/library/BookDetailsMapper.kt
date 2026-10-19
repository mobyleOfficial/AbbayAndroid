package com.mobyle.abbay.data.library

import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.model.BookDetails
import com.model.BookOverrides
import com.model.UnplayableFile

/** Library tables → [BookDetails]. */
internal object BookDetailsMapper {
    private const val UNSUPPORTED_EXTENSION = ".wma"

    fun toDetails(details: BookWithDetails): BookDetails {
        val book = details.book
        val playableTracks = BookProjection.playableTracks(details)
        val chapters = BookProjection.toBook(details, isRootAccessible = true)?.chapters.orEmpty()
        return BookDetails(
            id = book.id.toString(),
            title = book.displayTitle,
            author = book.displayAuthor,
            narrator = book.narratorOverride ?: book.narrator,
            description = book.descriptionOverride ?: book.description,
            coverUri = book.coverUri,
            durationMs = playableTracks.sumOf { it.durationMs },
            fileCount = playableTracks.size,
            location = if (book.rootId == null) playableTracks.firstOrNull()?.fileName.orEmpty() else book.relativePath,
            chapters = chapters,
            unplayableFiles = details.tracks.filter { it.unplayable }.sortedBy { it.indexInBook }.map {
                UnplayableFile(it.fileName, isUnsupportedFormat = it.fileName.endsWith(UNSUPPORTED_EXTENSION, ignoreCase = true))
            },
            sortByTrackNumber = details.listeningState?.sortByTrackTag == true,
            overrides = BookOverrides(book.titleOverride, book.authorOverride, book.narratorOverride, book.descriptionOverride),
        )
    }
}
