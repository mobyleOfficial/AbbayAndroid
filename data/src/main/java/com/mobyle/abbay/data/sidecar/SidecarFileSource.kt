package com.mobyle.abbay.data.sidecar

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.mobyle.abbay.data.storage.DocumentEntry
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.model.DataError
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * `<book folder>/.abbay/book.json` through the Storage Access Framework. Writes go to
 * `book.json.tmp` first and are renamed over the old file; providers without rename get the file written in place.
 */
class SidecarFileSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tree: DocumentTreeSource,
    private val contentSource: DocumentContentDataSource,
) {
    /** The sidecar text, or null when the `.abbay` folder holds none. */
    fun read(treeUri: String, sidecarFolderDocumentId: String): DataResult<String?> =
        when (val listing = tree.listChildren(treeUri, sidecarFolderDocumentId)) {
            is DataResult.Failure -> listing
            is DataResult.Success -> {
                val file = listing.value.firstOrNull { !it.isDirectory && it.name == FILE_NAME }
                if (file == null) {
                    DataResult.Success(null)
                } else {
                    when (val text = contentSource.readText(tree.documentUri(treeUri, file.documentId), MAXIMUM_BYTES)) {
                        is DataResult.Success -> DataResult.Success(text.value)
                        is DataResult.Failure -> text
                    }
                }
            }
        }

    /** Document id of the `.abbay` folder inside [bookFolderDocumentId], or null. */
    fun sidecarFolderOf(treeUri: String, bookFolderDocumentId: String): String? =
        childNamed(treeUri, bookFolderDocumentId, FOLDER_NAME, isDirectory = true)?.documentId

    /** Writes [text] as the sidecar of the book folder [bookFolderDocumentId], creating `.abbay` when needed. */
    fun write(treeUri: String, bookFolderDocumentId: String, text: String): DataResult<Unit> = guarded {
        val resolver = context.contentResolver
        val sidecarFolder = childNamed(treeUri, bookFolderDocumentId, FOLDER_NAME, isDirectory = true)
            ?.let { uriOf(treeUri, it.documentId) }
            ?: DocumentsContract.createDocument(
                resolver,
                uriOf(treeUri, bookFolderDocumentId),
                DocumentEntry.DIRECTORY_MIME_TYPE,
                FOLDER_NAME
            )
            ?: error("Cannot create $FOLDER_NAME")
        val sidecarFolderId = DocumentsContract.getDocumentId(sidecarFolder)
        val bytes = text.toByteArray(Charsets.UTF_8)

        // The temporary name has no .json extension, so the provider does not add one.
        val temporary = childNamed(treeUri, sidecarFolderId, TEMPORARY_NAME, isDirectory = false)?.let { uriOf(treeUri, it.documentId) }
            ?: DocumentsContract.createDocument(resolver, sidecarFolder, BINARY_MIME_TYPE, TEMPORARY_NAME)
            ?: error("Cannot create $TEMPORARY_NAME")
        resolver.openOutputStream(temporary, "wt")?.use { it.write(bytes) } ?: error("Cannot write $TEMPORARY_NAME")

        val existing = childNamed(treeUri, sidecarFolderId, FILE_NAME, isDirectory = false)?.let { uriOf(treeUri, it.documentId) }
        val renamed = runCatching {
            existing?.let { DocumentsContract.deleteDocument(resolver, it) }
            DocumentsContract.renameDocument(resolver, temporary, FILE_NAME)
        }.getOrNull()
        if (renamed == null) writeInPlace(treeUri, sidecarFolder, sidecarFolderId, temporary, bytes)
    }

    private fun writeInPlace(treeUri: String, sidecarFolder: Uri, sidecarFolderId: String, temporary: Uri, bytes: ByteArray) {
        val resolver = context.contentResolver
        val target = childNamed(treeUri, sidecarFolderId, FILE_NAME, isDirectory = false)?.let { uriOf(treeUri, it.documentId) }
            ?: DocumentsContract.createDocument(resolver, sidecarFolder, JSON_MIME_TYPE, FILE_NAME)
            ?: error("Cannot create $FILE_NAME")
        resolver.openOutputStream(target, "wt")?.use { it.write(bytes) } ?: error("Cannot write $FILE_NAME")
        runCatching { DocumentsContract.deleteDocument(resolver, temporary) }
    }

    private fun childNamed(treeUri: String, parentDocumentId: String, name: String, isDirectory: Boolean): DocumentEntry? =
        (tree.listChildren(treeUri, parentDocumentId) as? DataResult.Success)?.value
            ?.firstOrNull { it.name == name && it.isDirectory == isDirectory }

    private fun uriOf(treeUri: String, documentId: String): Uri = Uri.parse(tree.documentUri(treeUri, documentId))

    /** Data sources map every failure to a [DataResult]. */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private inline fun guarded(block: () -> Unit): DataResult<Unit> = try {
        block()
        DataResult.Success(Unit)
    } catch (securityException: SecurityException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (exception: Exception) {
        DataResult.Failure(DataError.UNREADABLE)
    }

    companion object {
        const val FOLDER_NAME = ".abbay"
        const val FILE_NAME = "book.json"
        private const val TEMPORARY_NAME = "book.json.tmp"
        private const val JSON_MIME_TYPE = "application/json"
        private const val BINARY_MIME_TYPE = "application/octet-stream"
        private const val MAXIMUM_BYTES = 1_048_576
    }
}
