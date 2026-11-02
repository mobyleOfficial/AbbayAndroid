package com.mobyle.abbay.data.readalong

import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.library.BookProjection
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongTranscript
import com.repository.ReadAlongRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadAlongRepositoryImpl @Inject constructor(
    private val store: ReadAlongTranscriptStore,
    private val settings: SettingsDataSource,
    private val dao: LibraryDao,
) : ReadAlongRepository {
    override suspend fun transcript(bookId: String): ReadAlongTranscript? = store.read(bookId)

    override suspend fun save(bookId: String, transcript: ReadAlongTranscript) = store.write(bookId, transcript)

    override suspend fun delete(bookId: String) = store.delete(bookId)

    override suspend fun deleteAll() = store.deleteAll()

    override suspend fun language(bookId: String): ReadAlongLanguage? {
        val code = settings.readAlongLanguage(bookId.toLongOrNull() ?: return null) ?: return null
        return ReadAlongLanguage.entries.firstOrNull { it.code == code }
    }

    override suspend fun setLanguage(bookId: String, language: ReadAlongLanguage?) {
        settings.setReadAlongLanguage(bookId.toLongOrNull() ?: return, language?.code)
    }

    /**
     * The book's files in listening order, one segment per track. A track that isn't on the device is a
     * segment with no file: the run stops there rather than fetching anything.
     */
    override suspend fun audioSource(bookId: String): ReadAlongAudioSource? {
        val details = dao.getBookWithDetails(bookId.toLongOrNull() ?: return null) ?: return null
        val tracks = BookProjection.playableTracks(details)
        if (tracks.isEmpty()) return null
        val segments = tracks.map { track ->
            ReadAlongAudioSource.Segment(
                fileUri = track.documentUri.takeIf(::isOnDevice),
                bookStartMs = track.startInBookMs,
                durationMs = track.durationMs,
            )
        }
        return ReadAlongAudioSource(segments = segments, isRemote = tracks.any { it.remoteUrl != null })
    }

    /** The library's own files (a folder the user granted) and LibriVox's downloads; anything else is a stream. */
    private fun isOnDevice(uri: String): Boolean = uri.startsWith("content:") || uri.startsWith("file:")
}
