package com.mobyle.abbay.data.readalong

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.readalong.ReadAlongEpub
import com.repository.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Which EPUB a book's read-along text comes from. One in the audiobook's folder wins: among the documents beside the
 * book in its library folder (last opened first), or beside a downloaded LibriVox book's files. Without one, the EPUB the
 * listener chose for the book from anywhere, kept as a persisted grant on its document so it stays readable.
 */
class ReadAlongEpubs @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val settings: SettingsDataSource,
    private val reader: EpubTextReader,
) {
    suspend fun epub(book: Book): ReadAlongEpub? = inBookFolder(book) ?: chosen(book.id)

    /** Keeps [uri] for the book once it reads as an EPUB (null forgets it); access to the one it replaces is let go. */
    suspend fun choose(bookId: String, uri: String?): Boolean {
        val previous = settings.readAlongEpub(bookId)
        val isAccepted = uri == null || isReadableEpub(uri)
        if (isAccepted) {
            settings.setReadAlongEpub(bookId, uri)
            previous?.takeIf { it != uri }?.let(::release)
        } else {
            uri?.let(::release)
        }
        return isAccepted
    }

    private suspend fun isReadableEpub(uri: String): Boolean =
        withContext(Dispatchers.IO) { runCatching { grant(uri) }.isSuccess } && reader.read(uri) != null

    private suspend fun inBookFolder(book: Book): ReadAlongEpub? {
        val companion = library.companionDocuments(book.id).firstOrNull { isEpub(it.name) }
        return companion?.let { ReadAlongEpub(it.uri, it.name, isInBookFolder = true) } ?: besideDownloads(book)
    }

    private suspend fun besideDownloads(book: Book): ReadAlongEpub? = withContext(Dispatchers.IO) {
        tracksOf(book).asSequence()
            .map { it.uri }
            .filter { it.startsWith("file:") }
            .mapNotNull { Uri.parse(it).path?.let(::File)?.parentFile }
            .distinct()
            .flatMap { folder -> folder.listFiles().orEmpty().filter { it.isFile && isEpub(it.name) }.sortedBy { it.name } }
            .firstOrNull()
            ?.let { ReadAlongEpub(Uri.fromFile(it).toString(), it.name, isInBookFolder = true) }
    }

    private suspend fun chosen(bookId: String): ReadAlongEpub? {
        val uri = settings.readAlongEpub(bookId) ?: return null
        return ReadAlongEpub(uri, withContext(Dispatchers.IO) { displayName(uri) }, isInBookFolder = false)
    }

    private fun grant(uri: String) =
        context.contentResolver.takePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)

    private fun release(uri: String) {
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    /** The file's name as the document provider shows it; the uri's last part when it can't say. */
    private fun displayName(uri: String): String {
        val parsed = Uri.parse(uri)
        val name = runCatching {
            context.contentResolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return name ?: parsed.lastPathSegment?.substringAfterLast('/') ?: uri
    }
}

private fun tracksOf(book: Book): List<BookFile> = when (book) {
    is MultipleBooks -> book.bookFileList
    is BookFile -> listOf(book)
    else -> emptyList()
}

private fun isEpub(name: String) = name.endsWith(".epub", ignoreCase = true)
