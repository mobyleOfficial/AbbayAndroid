package com.mobyle.abbay.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.mobyle.abbay.data.chapters.ByteSource
import com.model.DataError
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Small document reads and writes: description files, extracted covers, persisted permissions. */
@Suppress("TooManyFunctions") // One function per kind of document access.
class DocumentContentDataSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun readText(documentUri: String, maxBytes: Int): DataResult<String> = guarded {
        String(readUpTo(documentUri, maxBytes), Charsets.UTF_8).trim()
    }

    /** At most [maxBytes] of the document, undecoded (subtitles detect their own encoding). */
    fun readBytes(documentUri: String, maxBytes: Int): DataResult<ByteArray> = guarded { readUpTo(documentUri, maxBytes) }

    private fun readUpTo(documentUri: String, maxBytes: Int): ByteArray =
        context.contentResolver.openInputStream(Uri.parse(documentUri))?.use { input ->
            val bytes = ByteArray(maxBytes)
            var readCount = 0
            while (readCount < maxBytes) {
                val count = input.read(bytes, readCount, maxBytes - readCount)
                if (count < 0) break
                readCount += count
            }
            bytes.copyOf(readCount)
        } ?: ByteArray(0)

    /**
     * Runs [block] with positional read access to a document (a file channel over its descriptor), or returns a
     * failure when the document cannot be opened.
     */
    fun <T> withByteSource(documentUri: String, block: (ByteSource) -> T): DataResult<T> = guarded {
        val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(documentUri), "r") ?: error("No descriptor")
        descriptor.use {
            java.io.FileInputStream(it.fileDescriptor).channel.use { channel ->
                block(
                    object : ByteSource {
                        override val size: Long = channel.size()

                        override fun read(position: Long, length: Int): ByteArray {
                            if (position < 0 || position >= size || length <= 0) return ByteArray(0)
                            val buffer = java.nio.ByteBuffer.allocate(minOf(length.toLong(), size - position).toInt())
                            while (buffer.hasRemaining() && channel.read(buffer, position + buffer.position()) > 0) Unit
                            return buffer.array().copyOf(buffer.position())
                        }
                    },
                )
            }
        }
    }

    /** Stores an embedded cover in app storage and returns its `file://` URI. */
    fun saveCover(fileName: String, bytes: ByteArray): DataResult<String> = guarded {
        val coversDirectory = File(context.filesDir, COVERS_DIRECTORY).apply { mkdirs() }
        val coverFile = File(coversDirectory, fileName)
        coverFile.writeBytes(bytes)
        Uri.fromFile(coverFile).toString()
    }

    fun deleteCover(fileName: String) {
        runCatching { File(File(context.filesDir, COVERS_DIRECTORY), fileName).delete() }
    }

    /** Takes read and, when the provider allows it, write permission. */
    @Suppress("SwallowedException")
    fun takePersistablePermission(uri: String): DataResult<Unit> {
        val parsedUri = Uri.parse(uri)
        val readAndWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return guarded {
            try {
                context.contentResolver.takePersistableUriPermission(parsedUri, readAndWrite)
            } catch (writeNotGranted: SecurityException) {
                context.contentResolver.takePersistableUriPermission(parsedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun releasePersistablePermission(uri: String) {
        val readAndWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), readAndWrite) }
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    fun hasPersistedReadPermission(uri: String): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri.toString() == uri && it.isReadPermission }

    /** Display name and size of a single picked document. */
    fun describe(documentUri: String): DataResult<DocumentEntry> = guarded {
        context.contentResolver.query(Uri.parse(documentUri), null, null, null, null)?.use { cursor ->
            check(cursor.moveToFirst())
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            val modifiedColumn = cursor.getColumnIndex("last_modified")
            DocumentEntry(
                documentId = documentUri,
                name = if (nameColumn >= 0) cursor.getString(nameColumn).orEmpty() else documentUri.substringAfterLast('/'),
                mimeType = context.contentResolver.getType(Uri.parse(documentUri)).orEmpty(),
                lastModified = if (modifiedColumn >= 0 && !cursor.isNull(modifiedColumn)) cursor.getLong(modifiedColumn) else 0L,
                sizeBytes = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else 0L,
            )
        } ?: error("No row for $documentUri")
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
        const val COVERS_DIRECTORY = "covers"
    }
}
