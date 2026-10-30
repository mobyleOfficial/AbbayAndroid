package com.mobyle.abbay.data.library

import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.mobyle.abbay.data.scanner.ScannedBook
import java.util.UUID
import javax.inject.Inject

/**
 * Writes the structure of a [ScannedBook] into Room. A book keeps its row (and so its listening state) while its
 * relative path stays the same; tracks whose document, size and modification time did not change keep the tags read
 * before, so a rescan only reads new or changed files.
 */
class ScannedBookWriter @Inject constructor(private val dao: LibraryDao) {
    /** Returns the book id and whether its tags must be read again. */
    suspend fun write(location: BookLocation, scannedBook: ScannedBook, existingBook: BookEntity?, now: Long): WriteResult {
        val previousTracks = existingBook?.let { dao.getTracks(it.id) }.orEmpty().sortedBy { it.indexInBook }
        val newTracks = buildTracks(location, scannedBook, existingBook?.id ?: 0L, previousTracks)
        val tracksUnchanged = existingBook != null &&
            newTracks.map { it.documentUri } == previousTracks.map { it.documentUri } &&
            newTracks.all { it.metadataRead }
        val needsMetadata = !tracksUnchanged || existingBook?.metadataComplete != true

        val bookId = if (existingBook == null) {
            dao.insertBook(newBook(location, scannedBook, now))
        } else {
            dao.updateBook(
                existingBook.copy(
                    rootId = location.rootId,
                    relativePath = scannedBook.relativePath,
                    folderUri = location.folderUri,
                    folderDocumentId = scannedBook.folderDocumentId,
                    trackCount = scannedBook.tracks.size,
                    isSingleFile = scannedBook.isSingleFile,
                    lastScannedAt = now,
                    folderLastModified = scannedBook.folderLastModified,
                    missingSince = null,
                    metadataComplete = existingBook.metadataComplete && !needsMetadata,
                ),
            )
            existingBook.id
        }

        if (!tracksUnchanged) dao.replaceTracks(bookId, newTracks.map { it.copy(bookId = bookId) })

        return WriteResult(bookId = bookId, needsMetadata = needsMetadata)
    }

    private fun buildTracks(
        location: BookLocation,
        scannedBook: ScannedBook,
        bookId: Long,
        previousTracks: List<TrackEntity>,
    ): List<TrackEntity> {
        val previousByUri = previousTracks.associateBy { it.documentUri }
        val files = scannedBook.tracks.map { it to false } + scannedBook.unsupportedFiles.map { it to true }

        return files.mapIndexed { index, (file, isUnsupported) ->
            val documentUri = location.documentUriOf(file.documentId)
            val previous = previousByUri[documentUri]
                ?.takeIf { it.sizeBytes == file.sizeBytes && it.lastModified == file.lastModified && it.metadataRead }
            TrackEntity(
                bookId = bookId,
                indexInBook = index,
                documentUri = documentUri,
                fileName = file.name,
                sizeBytes = file.sizeBytes,
                lastModified = file.lastModified,
                durationMs = previous?.durationMs ?: 0L,
                startInBookMs = previous?.startInBookMs ?: 0L,
                titleTag = previous?.titleTag,
                trackNumberTag = previous?.trackNumberTag,
                unplayable = isUnsupported || previous?.unplayable == true,
                metadataRead = isUnsupported || previous != null,
            )
        }
    }

    private fun newBook(location: BookLocation, scannedBook: ScannedBook, now: Long) = BookEntity(
        rootId = location.rootId,
        relativePath = scannedBook.relativePath,
        folderUri = location.folderUri,
        folderDocumentId = scannedBook.folderDocumentId,
        title = provisionalTitle(scannedBook),
        author = null,
        narrator = null,
        description = null,
        coverUri = null,
        coverSource = null,
        durationMs = 0L,
        trackCount = scannedBook.tracks.size,
        isSingleFile = scannedBook.isSingleFile,
        addedAt = now,
        lastScannedAt = now,
        folderLastModified = scannedBook.folderLastModified,
        sidecarId = UUID.randomUUID().toString(),
    )

    data class WriteResult(val bookId: Long, val needsMetadata: Boolean)

    companion object {
        /** Title used until tags are read: the folder name (for a one-file book, the file name without extension). */
        fun provisionalTitle(scannedBook: ScannedBook): String = scannedBook.folderName
    }
}
