package com.mobyle.abbay.data.readalong

import android.content.Context
import android.net.Uri
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongTranscript
import com.repository.ReadAlongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ReadAlongRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ReadAlongTranscriptStore,
    private val models: VoskModelInstaller,
    private val settings: SettingsDataSource,
) : ReadAlongRepository {
    override suspend fun load(bookId: String) = store.load(bookId)

    override suspend fun save(bookId: String, transcript: ReadAlongTranscript, stamp: Int) = store.save(bookId, transcript, stamp)

    override suspend fun delete(bookId: String) = store.delete(bookId)

    override suspend fun deleteAll() = store.deleteAll()

    override suspend fun deleteModels() = models.deleteAll()

    override suspend fun language(bookId: String): String? = settings.readAlongLanguage(bookId)

    override suspend fun setLanguage(bookId: String, locale: String?) = settings.setReadAlongLanguage(bookId, locale)

    override suspend fun syncOffset(bookId: String): Long = settings.readAlongSyncOffset(bookId)

    override suspend fun setSyncOffset(bookId: String, offsetMs: Long) = settings.setReadAlongSyncOffset(bookId, offsetMs)

    override suspend fun forgetBooks() = settings.clearReadAlongBookSettings()

    /**
     * The book's files in book time. A file streamed from a server (a LibriVox or Audiobookshelf chapter not
     * downloaded) has no uri; so has a local file that can't be opened any more (moved, or its folder's access lost).
     */
    override suspend fun audioSource(book: Book): ReadAlongAudioSource = withContext(Dispatchers.IO) {
        val tracks = when (book) {
            is MultipleBooks -> book.bookFileList
            is BookFile -> listOf(book)
            else -> emptyList()
        }
        var start = 0L
        val segments = tracks.map { track ->
            val duration = if (tracks.size == 1) maxOf(track.duration, book.duration) else track.duration
            ReadAlongAudioSource.Segment(uri = track.uri.takeIf(::isReadable), bookStartMs = start, durationMs = duration).also {
                start += duration
            }
        }
        ReadAlongAudioSource(segments, isDownloadable = tracks.any { isRemote(it.uri) })
    }

    private fun isRemote(uri: String) = uri.startsWith("http://") || uri.startsWith("https://")

    private fun isReadable(uri: String): Boolean = when {
        uri.isBlank() || isRemote(uri) -> false
        uri.startsWith("file:") -> Uri.parse(uri).path?.let { File(it).canRead() } == true
        else -> runCatching { context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } == true }.getOrDefault(false)
    }
}
