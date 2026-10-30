package com.mobyle.abbay.data.library

import com.mobyle.abbay.data.chapters.BookChapterBuilder
import com.mobyle.abbay.data.chapters.ChapterReader
import com.mobyle.abbay.data.chapters.CueSheetParser
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ChapterEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.mobyle.abbay.data.scanner.FileKinds
import com.mobyle.abbay.data.scanner.ScannedBook
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.mobyle.abbay.data.storage.TrackMetadata
import com.mobyle.abbay.data.storage.TrackMetadataDataSource
import com.model.DataResult
import javax.inject.Inject

class BookMetadataEnricher @Inject constructor(
    private val dao: LibraryDao,
    private val metadataSource: TrackMetadataDataSource,
    private val contentSource: DocumentContentDataSource,
) {
    suspend fun enrich(bookId: Long, scannedBook: ScannedBook, documentUriOf: (documentId: String) -> String) {
        val book = dao.getBook(bookId) ?: return
        val (readTracks, tags) = readTracks(dao.getTracks(bookId))
        val tracks = positioned(readTracks)
        dao.replaceTracks(bookId, tracks)

        val playableTracks = tracks.filterNot { it.unplayable }
        dao.replaceChapters(bookId, chaptersOf(bookId, scannedBook, playableTracks, documentUriOf))
        val title = tags?.album ?: tags?.title?.takeIf { scannedBook.isSingleFile } ?: ScannedBookWriter.provisionalTitle(scannedBook)
        val description = scannedBook.descriptionFile?.let { file ->
            (contentSource.readText(documentUriOf(file.documentId), MAX_DESCRIPTION_BYTES) as? DataResult.Success)
                ?.value?.takeIf(String::isNotBlank)
        }
        val cover = resolveCover(book, scannedBook, playableTracks, documentUriOf)

        dao.updateBook(
            book.copy(
                title = title,
                author = tags?.albumArtist ?: tags?.artist ?: scannedBook.parentFolderName,
                narrator = tags?.composer,
                description = description,
                coverUri = cover?.uri,
                coverSource = cover?.source,
                durationMs = playableTracks.sumOf { it.durationMs },
                trackCount = playableTracks.size,
                metadataComplete = true,
            ),
        )
    }

    private fun readTracks(storedTracks: List<TrackEntity>): Pair<List<TrackEntity>, TrackMetadata?> {
        var firstTrackTags: TrackMetadata? = null
        val tracks = storedTracks.map { track ->
            val alreadyKnown = track.metadataRead && (firstTrackTags != null || track.unplayable)
            if (alreadyKnown) return@map track

            when (val result = metadataSource.read(track.documentUri)) {
                is DataResult.Failure -> track.copy(unplayable = true, metadataRead = true)
                is DataResult.Success -> {
                    val isPlayable = result.value.hasAudio && !track.unplayable
                    if (isPlayable && firstTrackTags == null) firstTrackTags = result.value
                    track.copy(
                        durationMs = exactMp3Duration(track) ?: result.value.durationMs,
                        titleTag = result.value.title,
                        trackNumberTag = result.value.trackNumber,
                        unplayable = !isPlayable,
                        metadataRead = true,
                    )
                }
            }
        }
        return tracks to firstTrackTags
    }

    private fun chaptersOf(
        bookId: Long,
        scannedBook: ScannedBook,
        playableTracks: List<TrackEntity>,
        documentUriOf: (documentId: String) -> String,
    ): List<ChapterEntity> {
        val cueChapters = scannedBook.cueSheet
            ?.let { contentSource.readText(documentUriOf(it.documentId), MAX_CUE_BYTES) as? DataResult.Success }
            ?.let { CueSheetParser.parse(it.value) }
        val byFileName = playableTracks.associateBy { it.fileName }
        val chapters = BookChapterBuilder.build(
            tracks = playableTracks.map { BookChapterBuilder.Track(it.fileName, it.startInBookMs, it.durationMs, it.titleTag) },
            cueChapters = cueChapters,
            embeddedChapters = { track ->
                val documentUri = byFileName[track.fileName]?.documentUri
                val result = documentUri?.let { uri ->
                    contentSource.withByteSource(uri) { source -> ChapterReader.embeddedChapters(track.fileName, source) }
                }
                (result as? DataResult.Success)?.value.orEmpty()
            },
        )
        return chapters.mapIndexed { index, chapter ->
            ChapterEntity(
                bookId = bookId,
                indexInBook = index,
                title = chapter.title,
                startMs = chapter.startMs,
                endMs = chapter.endMs,
                source = chapter.source,
            )
        }
    }

    private fun exactMp3Duration(track: TrackEntity): Long? {
        if (!track.fileName.endsWith(MP3_EXTENSION, ignoreCase = true)) return null
        return (metadataSource.readExactMp3Duration(track.documentUri) as? DataResult.Success)?.value
    }

    private fun positioned(tracks: List<TrackEntity>): List<TrackEntity> {
        var nextStart = 0L
        return tracks.map { track ->
            val positionedTrack = track.copy(startInBookMs = nextStart)
            if (!track.unplayable) nextStart += track.durationMs
            positionedTrack
        }
    }

    private fun resolveCover(
        book: BookEntity,
        scannedBook: ScannedBook,
        playableTracks: List<TrackEntity>,
        documentUriOf: (documentId: String) -> String,
    ): Cover? {
        if (book.coverSource == COVER_SOURCE_USER) return book.coverUri?.let { Cover(it, COVER_SOURCE_USER) }

        val folderImage = FileKinds.preferredCover(scannedBook.images)
        if (folderImage != null) contentSource.deleteCover(book.sidecarId)

        return folderImage?.let { Cover(documentUriOf(it.documentId), COVER_SOURCE_FOLDER_IMAGE) }
            ?: embeddedCover(book.sidecarId, playableTracks)
    }

    private fun embeddedCover(coverFileName: String, playableTracks: List<TrackEntity>): Cover? =
        playableTracks.take(MAX_TRACKS_CHECKED_FOR_EMBEDDED_COVER).firstNotNullOfOrNull { track ->
            val picture = (metadataSource.readEmbeddedPicture(track.documentUri) as? DataResult.Success)?.value
            picture?.takeIf { it.isNotEmpty() }
                ?.let { (contentSource.saveCover(coverFileName, it) as? DataResult.Success)?.value }
                ?.let { Cover(it, COVER_SOURCE_EMBEDDED) }
        }

    private data class Cover(val uri: String, val source: String)

    companion object {
        const val COVER_SOURCE_USER = "USER"
        const val COVER_SOURCE_FOLDER_IMAGE = "FOLDER_IMAGE"
        const val COVER_SOURCE_EMBEDDED = "EMBEDDED"
        private const val MAX_DESCRIPTION_BYTES = 16 * 1024
        private const val MAX_TRACKS_CHECKED_FOR_EMBEDDED_COVER = 3
        private const val MP3_EXTENSION = ".mp3"
        private const val MAX_CUE_BYTES = 256 * 1024
    }
}
