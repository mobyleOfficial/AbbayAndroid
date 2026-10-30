package com.mobyle.abbay.data.storage

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.model.DataError
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject

class SafDocumentTreeDataSource @Inject constructor(@ApplicationContext private val context: Context) : DocumentTreeSource {
    override fun rootDocumentId(treeUri: String): String = DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))

    override fun rootEntry(treeUri: String): DataResult<DocumentEntry> = guarded {
        val tree = Uri.parse(treeUri)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(documentUri, PROJECTION, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toEntry() else null
        } ?: throw FileNotFoundException(treeUri)
    }

    override fun listChildren(treeUri: String, parentDocumentId: String): DataResult<List<DocumentEntry>> = guarded {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(treeUri), parentDocumentId)
        context.contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntry()) }
        } ?: throw FileNotFoundException(parentDocumentId)
    }

    override fun documentUri(treeUri: String, documentId: String): String =
        DocumentsContract.buildDocumentUriUsingTree(Uri.parse(treeUri), documentId).toString()

    private fun Cursor.toEntry() = DocumentEntry(
        documentId = getString(COLUMN_DOCUMENT_ID),
        name = getString(COLUMN_DISPLAY_NAME).orEmpty(),
        mimeType = getString(COLUMN_MIME_TYPE).orEmpty(),
        lastModified = if (isNull(COLUMN_LAST_MODIFIED)) 0L else getLong(COLUMN_LAST_MODIFIED),
        sizeBytes = if (isNull(COLUMN_SIZE)) 0L else getLong(COLUMN_SIZE),
    )

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private inline fun <T> guarded(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (securityException: SecurityException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (notFound: FileNotFoundException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (illegalArgument: IllegalArgumentException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (exception: Exception) {
        DataResult.Failure(DataError.UNREADABLE)
    }

    private companion object {
        const val COLUMN_DOCUMENT_ID = 0
        const val COLUMN_DISPLAY_NAME = 1
        const val COLUMN_MIME_TYPE = 2
        const val COLUMN_LAST_MODIFIED = 3
        const val COLUMN_SIZE = 4
        val PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_SIZE,
        )
    }
}
