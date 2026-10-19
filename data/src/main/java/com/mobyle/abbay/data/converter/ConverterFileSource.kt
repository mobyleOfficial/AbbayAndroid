package com.mobyle.abbay.data.converter

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.DocumentsContract
import com.mobyle.abbay.data.scanner.FileKinds
import com.mobyle.abbay.data.storage.DocumentEntry
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import javax.inject.Inject

/** The SAF operations of the converter. Every call may throw; the runner maps it. */
@Suppress("TooManyFunctions") // One function per SAF operation of the converter.
class ConverterFileSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tree: DocumentTreeSource,
) {
    private val resolver get() = context.contentResolver

    /** A file of the book folder or of one of its sub-folders (discs). */
    class Located(val entry: DocumentEntry, val parentDocumentId: String, val subFolderName: String?)

    /** Finds the document behind [trackUri] in [bookFolderId] or one level below. */
    fun locate(treeUri: String, bookFolderId: String, trackUri: String): Located? {
        val top = children(treeUri, bookFolderId)
        top.firstOrNull { uriOf(treeUri, it.documentId) == trackUri }?.let { return Located(it, bookFolderId, null) }
        return top.filter { it.isDirectory && !FileKinds.isIgnored(it) }.firstNotNullOfOrNull { folder ->
            children(treeUri, folder.documentId).firstOrNull { uriOf(treeUri, it.documentId) == trackUri }
                ?.let { Located(it, folder.documentId, folder.name) }
        }
    }

    fun children(treeUri: String, folderId: String): List<DocumentEntry> =
        (tree.listChildren(treeUri, folderId) as? DataResult.Success)?.value.orEmpty()

    /** A new, empty `<name>`; an old one (an interrupted run) is deleted first. No MIME extension is added. */
    fun createFresh(treeUri: String, parentId: String, name: String): Uri {
        children(treeUri, parentId).firstOrNull { it.name == name }?.let { delete(treeUri, it.documentId) }
        return DocumentsContract.createDocument(resolver, Uri.parse(uriOf(treeUri, parentId)), BINARY_MIME_TYPE, name)
            ?: error("Cannot create a file")
    }

    fun openOutput(uri: Uri): OutputStream = resolver.openOutputStream(uri, "w") ?: error("Cannot write")

    /** Duration (µs) and MIME type of the audio track of [uri], read back with MediaExtractor. */
    fun probe(uri: Uri): Pair<String?, Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val format = extractor.getTrackFormat(0)
            val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else -1L
            return format.getString(MediaFormat.KEY_MIME) to duration
        } finally {
            extractor.release()
        }
    }

    fun rename(uri: Uri, name: String): Uri = DocumentsContract.renameDocument(resolver, uri, name) ?: uri

    fun sizeOf(uri: Uri): Long = resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L

    fun delete(treeUri: String, documentId: String) {
        DocumentsContract.deleteDocument(resolver, Uri.parse(uriOf(treeUri, documentId)))
    }

    /**
     * Moves [file] into `.abbay/originals` of the book folder (in a sub-folder of the same name for disc folders).
     * Providers that cannot move get a copy and a delete.
     */
    fun moveToOriginals(treeUri: String, bookFolderId: String, file: Located) {
        val abbay = folder(treeUri, bookFolderId, FileKinds.SIDECAR_FOLDER)
        val originals = folder(treeUri, abbay, ORIGINALS_FOLDER)
        val target = file.subFolderName?.let { folder(treeUri, originals, it) } ?: originals
        children(treeUri, target).firstOrNull { it.name == file.entry.name }?.let { delete(treeUri, it.documentId) }
        val source = Uri.parse(uriOf(treeUri, file.entry.documentId))
        val moved = runCatching {
            DocumentsContract.moveDocument(
                resolver,
                source,
                Uri.parse(uriOf(treeUri, file.parentDocumentId)),
                Uri.parse(uriOf(treeUri, target))
            )
        }.getOrNull()
        if (moved == null) {
            val copy = DocumentsContract.createDocument(resolver, Uri.parse(uriOf(treeUri, target)), BINARY_MIME_TYPE, file.entry.name)
                ?: error("Cannot copy the original")
            resolver.openInputStream(source)?.use { input -> openOutput(copy).use(input::copyTo) } ?: error("Cannot read the original")
            DocumentsContract.deleteDocument(resolver, source)
        }
    }

    private fun folder(treeUri: String, parentId: String, name: String): String =
        children(treeUri, parentId).firstOrNull { it.isDirectory && it.name == name }?.documentId
            ?: DocumentsContract.createDocument(resolver, Uri.parse(uriOf(treeUri, parentId)), DocumentEntry.DIRECTORY_MIME_TYPE, name)
                ?.let(DocumentsContract::getDocumentId)
            ?: error("Cannot create $name")

    private fun uriOf(treeUri: String, documentId: String) = tree.documentUri(treeUri, documentId)

    private companion object {
        const val BINARY_MIME_TYPE = "application/octet-stream"
        const val ORIGINALS_FOLDER = "originals"
    }
}
