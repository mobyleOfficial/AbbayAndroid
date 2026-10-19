package com.mobyle.abbay.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import com.model.DataError
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Turns an image picked or shared by the user into a JPEG cover and writes it to `<book>/.abbay/cover.jpg`
 *. Covers are downsampled to at most 1500 px.
 */
class CoverImageWriter @Inject constructor(@ApplicationContext private val context: Context) {
    /** Reads and re-encodes the image at [imageUri] as a JPEG. */
    fun readAsJpeg(imageUri: String): DataResult<ByteArray> = guarded {
        val uri = Uri.parse(imageUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sampleSize = generateSequence(1) { it * 2 }
            .first { maxOf(bounds.outWidth, bounds.outHeight) / it <= MAX_COVER_SIZE_PX }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: error("Not an image")
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    /**
     * Writes [jpeg] as `.abbay/cover.jpg` inside the folder [folderDocumentId] of [treeUri], creating the `.abbay` folder
     * when needed. Returns the cover's document URI; fails when the folder is read-only.
     */
    fun writeIntoBookFolder(treeUri: String, folderDocumentId: String, children: List<DocumentEntry>, jpeg: ByteArray): DataResult<String> =
        guarded {
            val tree = Uri.parse(treeUri)
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(tree, folderDocumentId)
            val abbayFolder = children.firstOrNull { it.isDirectory && it.name == SIDECAR_FOLDER }
                ?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.documentId) }
                ?: DocumentsContract.createDocument(context.contentResolver, folderUri, DocumentEntry.DIRECTORY_MIME_TYPE, SIDECAR_FOLDER)
                ?: error("Cannot create $SIDECAR_FOLDER")
            val existingCover = findChild(tree, abbayFolder, COVER_FILE_NAME)
            val coverUri = existingCover
                ?: DocumentsContract.createDocument(context.contentResolver, abbayFolder, JPEG_MIME_TYPE, COVER_FILE_NAME)
                ?: error("Cannot create $COVER_FILE_NAME")
            context.contentResolver.openOutputStream(coverUri, "wt")?.use { it.write(jpeg) } ?: error("Cannot write cover")
            coverUri.toString()
        }

    private fun findChild(tree: Uri, parent: Uri, name: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(0) to cursor.getString(1) else null }
                .firstOrNull { (_, displayName) -> displayName == name }
                ?.let { (documentId, _) -> DocumentsContract.buildDocumentUriUsingTree(tree, documentId) }
        }
    }

    /** Data sources map every failure to a [DataResult]. */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private inline fun <T> guarded(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (securityException: SecurityException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (exception: Exception) {
        DataResult.Failure(DataError.UNREADABLE)
    }

    private companion object {
        const val SIDECAR_FOLDER = ".abbay"
        const val COVER_FILE_NAME = "cover.jpg"
        const val JPEG_MIME_TYPE = "image/jpeg"
        const val MAX_COVER_SIZE_PX = 1500
        const val JPEG_QUALITY = 90
    }
}
