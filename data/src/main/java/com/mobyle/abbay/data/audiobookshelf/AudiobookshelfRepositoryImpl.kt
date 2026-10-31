package com.mobyle.abbay.data.audiobookshelf

import androidx.work.ExistingWorkPolicy
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ChapterEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.mobyle.abbay.data.librivox.LibriVoxDownloadControl
import com.mobyle.abbay.data.librivox.LibriVoxDownloadScheduler
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfPage
import com.model.AudiobookshelfSeries
import com.model.AudiobookshelfSignIn
import com.model.AudiobookshelfSort
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import com.model.LibriVoxDownloadMode
import com.repository.AudiobookshelfRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookshelfRepositoryImpl @Inject constructor(
    private val api: AudiobookshelfApi,
    private val signInClient: AudiobookshelfSignInClient,
    private val session: AudiobookshelfSession,
    private val dao: LibraryDao,
    private val scheduler: LibriVoxDownloadScheduler,
    private val control: LibriVoxDownloadControl,
    private val contentSource: DocumentContentDataSource,
) : AudiobookshelfRepository {
    override fun observeAccount(): Flow<AudiobookshelfAccount?> = session.account

    override suspend fun signIn(serverUrl: String, username: String, password: String): AudiobookshelfSignIn =
        withContext(Dispatchers.IO) { signInClient.signIn(serverUrl, username, password) }

    override suspend fun signOut() = withContext(Dispatchers.IO) { session.signOut() }

    override suspend fun libraries(): DataResult<List<AudiobookshelfLibrary>> = guarded { api.libraries() }

    override suspend fun books(libraryId: String, sort: AudiobookshelfSort, page: Int): DataResult<AudiobookshelfPage<LibriVoxBook>> =
        guarded { api.books(libraryId, sort, page) }

    override suspend fun search(libraryId: String, query: String): DataResult<List<LibriVoxBook>> {
        val text = query.trim()
        if (text.isEmpty()) return DataResult.Success(emptyList())
        return guarded { api.search(libraryId, text) }
    }

    override suspend fun series(libraryId: String, page: Int): DataResult<AudiobookshelfPage<AudiobookshelfSeries>> =
        guarded { api.series(libraryId, page) }

    override suspend fun seriesBooks(libraryId: String, seriesId: String): DataResult<List<LibriVoxBook>> =
        guarded { api.seriesBooks(libraryId, seriesId) }

    override fun coverUrlOf(itemId: String): String = api.coverUrlOf(itemId)

    override fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>> = flow<DataResult<LibriVoxDetails>> {
        val fetched = api.book(book.id) ?: throw NotAnAudiobookException()
        emit(DataResult.Success(LibriVoxDetails(fetched, isAwaitingTitles = false)))
    }
        .flowOn(Dispatchers.IO)
        .catch { error -> emit(DataResult.Failure(errorOf(error))) }

    override fun observeLibraryBookIds(): Flow<Map<String, String>> = dao.observeLibriVoxBooks()
        .map { books ->
            books.mapNotNull { details ->
                details.book.librivoxId?.let(AudiobookshelfUrls::itemIdOf)?.let { it to details.book.id.toString() }
            }.toMap()
        }
        .distinctUntilChanged()

    override suspend fun add(book: LibriVoxBook, allowsCellular: Boolean, download: Boolean): DataResult<String> =
        withContext(Dispatchers.IO) {
            if (book.sections.isEmpty()) return@withContext DataResult.Failure(DataError.NOT_AN_AUDIOBOOK)
            val libraryId = AudiobookshelfUrls.libraryId(book.id)
            dao.getBookByLibriVoxId(libraryId)?.let { existing ->
                if (existing.hidden) dao.updateBook(existing.copy(hidden = false))
                if (download) control.start(existing.id, allowsCellular)
                return@withContext DataResult.Success(existing.id.toString())
            }
            val serverUrl = session.credentials()?.serverUrl ?: return@withContext DataResult.Failure(DataError.NETWORK)
            val sidecarId = UUID.randomUUID().toString()
            val coverUri = api.coverBytes(book)?.let { bytes -> (contentSource.saveCover(sidecarId, bytes) as? DataResult.Success)?.value }
            val now = System.currentTimeMillis()
            val entity = BookEntity(
                rootId = null,
                relativePath = RELATIVE_PATH_PREFIX + book.id,
                folderUri = AudiobookshelfUrls.itemPage(serverUrl, book.id),
                folderDocumentId = libraryId,
                title = book.title,
                author = book.authors.takeIf(String::isNotBlank),
                narrator = null,
                description = book.description.takeIf(String::isNotBlank),
                coverUri = coverUri,
                coverSource = coverUri?.let { COVER_SOURCE },
                durationMs = book.durationMs,
                trackCount = book.sections.size,
                isSingleFile = book.sections.size == 1,
                addedAt = now,
                lastScannedAt = now,
                folderLastModified = 0L,
                metadataComplete = true,
                sidecarId = sidecarId,
                librivoxId = libraryId,
                librivoxDownload = (if (download) LibriVoxDownloadMode.ACTIVE else LibriVoxDownloadMode.OFF).name,
            )
            val bookId = dao.insertBookWithTracks(entity, tracksOf(book))
            chaptersOf(bookId, book).takeIf { it.isNotEmpty() }?.let { dao.insertChapters(it) }
            if (download) scheduler.enqueue(bookId, allowsCellular, ExistingWorkPolicy.KEEP)
            DataResult.Success(bookId.toString())
        }

    private fun tracksOf(book: LibriVoxBook): List<TrackEntity> {
        var start = 0L
        return book.sections.mapIndexed { index, section ->
            TrackEntity(
                bookId = 0L,
                indexInBook = index,
                documentUri = section.listenUrl,
                fileName = section.fileName.ifEmpty { section.listenUrl.substringAfterLast('/') },
                sizeBytes = section.sizeBytes,
                lastModified = 0L,
                durationMs = section.durationMs,
                startInBookMs = start,
                titleTag = section.title,
                trackNumberTag = index + 1,
                unplayable = false,
                metadataRead = true,
                remoteUrl = section.listenUrl,
            ).also { start += section.durationMs }
        }
    }

    /** The server's chapters, when it knows more than one per file: otherwise each file is a chapter already. */
    private fun chaptersOf(bookId: Long, book: LibriVoxBook): List<ChapterEntity> {
        if (book.chapters.size <= 1 && book.sections.size > 1) return emptyList()
        return book.chapters.mapIndexed { index, chapter ->
            ChapterEntity(
                bookId = bookId,
                indexInBook = index,
                title = chapter.title,
                startMs = chapter.startMs,
                endMs = chapter.endMs,
                source = CHAPTER_SOURCE,
            )
        }
    }

    private suspend fun <T> guarded(block: suspend () -> T): DataResult<T> = try {
        DataResult.Success(withContext(Dispatchers.IO) { block() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        DataResult.Failure(errorOf(error))
    }

    private fun errorOf(error: Throwable): DataError = when (error) {
        is NotAnAudiobookException -> DataError.NOT_AN_AUDIOBOOK
        is IOException -> DataError.NETWORK
        else -> DataError.UNREADABLE
    }

    private class NotAnAudiobookException : Exception()

    companion object {
        const val RELATIVE_PATH_PREFIX = "audiobookshelf:"
        private const val COVER_SOURCE = "audiobookshelf"
        private const val CHAPTER_SOURCE = "AUDIOBOOKSHELF"
    }
}
