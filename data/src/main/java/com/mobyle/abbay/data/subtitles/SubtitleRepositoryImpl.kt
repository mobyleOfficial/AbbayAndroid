package com.mobyle.abbay.data.subtitles

import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.library.BookProjection
import com.mobyle.abbay.data.scanner.FileKinds
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.mobyle.abbay.data.storage.DocumentEntry
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.model.DataResult
import com.repository.SubtitleRepository
import com.subtitles.TrackSubtitles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds `<track base name>.srt` or `.lrc` in the folder of the track (the book folder or one of its disc folders) and
 * parses it. Files are read once per book, track and charset.
 */
@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    private val dao: LibraryDao,
    private val tree: DocumentTreeSource,
    private val content: DocumentContentDataSource,
    private val settings: SettingsDataSource,
) : SubtitleRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val cache = mutableMapOf<Triple<Long, Int, String?>, TrackSubtitles?>()

    override suspend fun subtitles(bookId: String, trackIndex: Int): TrackSubtitles? = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext null
        val charset = settings.subtitleCharset(id)
        val key = Triple(id, trackIndex, charset)
        synchronized(cache) { if (key in cache) return@withContext cache[key] }
        val result = load(id, trackIndex, charset)
        synchronized(cache) { cache[key] = result }
        result
    }

    override suspend fun setCharset(bookId: String, charset: String?) {
        val id = bookId.toLongOrNull() ?: return
        settings.setSubtitleCharset(id, charset)
    }

    private suspend fun load(bookId: Long, trackIndex: Int, charset: String?): TrackSubtitles? {
        val (treeUri, subtitleFile) = findFile(bookId, trackIndex) ?: return null
        val bytes = (content.readBytes(tree.documentUri(treeUri, subtitleFile.documentId), MAX_BYTES) as? DataResult.Success)?.value
        return bytes?.let { decodeFile(subtitleFile.name, it, charset) }
    }

    private fun decodeFile(fileName: String, bytes: ByteArray, charset: String?): TrackSubtitles =
        SubtitleParser.decode(bytes, charset)?.let { text ->
            TrackSubtitles(fileName, SubtitleParser.parse(fileName, text), charset, needsCharset = false)
        }
            ?: TrackSubtitles(fileName, emptyList(), charset = null, needsCharset = true)

    /** The tree of the book's root and the subtitle file next to its playable track [trackIndex]. */
    private suspend fun findFile(bookId: Long, trackIndex: Int): Pair<String, DocumentEntry>? {
        val details = dao.getBookWithDetails(bookId) ?: return null
        val track = BookProjection.playableTracks(details).getOrNull(trackIndex)
        val root = details.book.rootId?.let { rootId -> dao.getRoots().firstOrNull { it.id == rootId } }
        if (track == null || root == null) return null
        return siblingsOf(root.treeUri, details.book.folderDocumentId, track.documentUri)
            ?.let { siblings -> subtitleFor(track.fileName, siblings) }
            ?.let { root.treeUri to it }
    }

    /** The listing of the folder that holds the track: the book folder, or a sub-folder (multi-disc books). */
    private fun siblingsOf(treeUri: String, bookFolderId: String, trackUri: String): List<DocumentEntry>? {
        val top = (tree.listChildren(treeUri, bookFolderId) as? DataResult.Success)?.value ?: return null
        if (top.any { tree.documentUri(treeUri, it.documentId) == trackUri }) return top
        return top.filter { it.isDirectory && !FileKinds.isIgnored(it) }.firstNotNullOfOrNull { folder ->
            (tree.listChildren(treeUri, folder.documentId) as? DataResult.Success)?.value
                ?.takeIf { children -> children.any { tree.documentUri(treeUri, it.documentId) == trackUri } }
        }
    }

    private fun subtitleFor(trackFileName: String, siblings: List<DocumentEntry>): DocumentEntry? {
        val baseName = FileKinds.baseName(trackFileName).lowercase()
        return EXTENSIONS.firstNotNullOfOrNull { extension ->
            siblings.firstOrNull { !it.isDirectory && it.name.lowercase() == "$baseName.$extension" }
        }
    }

    private companion object {
        val EXTENSIONS = listOf("srt", "lrc")
        const val MAX_BYTES = 4 * 1024 * 1024
    }
}
