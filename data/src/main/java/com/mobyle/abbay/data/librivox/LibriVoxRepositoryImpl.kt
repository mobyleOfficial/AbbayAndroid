package com.mobyle.abbay.data.librivox

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.model.ChapterDownload
import com.model.ChapterDownloadStatus
import com.model.DataError
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import com.model.LibriVoxDownload
import com.model.LibriVoxDownloadMode
import com.model.NetworkStatus
import com.repository.LibriVoxRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibriVoxRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: LibriVoxCatalog,
    private val dao: LibraryDao,
    private val scheduler: LibriVoxDownloadScheduler,
    private val tracker: LibriVoxDownloadTracker,
    private val control: LibriVoxDownloadControl,
    private val contentSource: DocumentContentDataSource,
) : LibriVoxRepository {
    override suspend fun popular(): DataResult<List<LibriVoxBook>> = guarded { catalog.popular() }

    override suspend fun search(query: String): DataResult<List<LibriVoxBook>> = guarded { catalog.search(query) }

    override fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>> = catalog.details(book)
        .map<LibriVoxDetails, DataResult<LibriVoxDetails>> { DataResult.Success(it) }
        .catch { error -> emit(DataResult.Failure(errorOf(error))) }

    override fun observeLibraryBookIds(): Flow<Map<String, String>> = dao.observeLibriVoxBooks()
        .map { books -> books.mapNotNull { details -> details.book.librivoxId?.let { it to details.book.id.toString() } }.toMap() }
        .distinctUntilChanged()

    override fun observeDownloads(): Flow<Map<String, LibriVoxDownload>> =
        combine(dao.observeLibriVoxBooks(), tracker.state) { books, active ->
            books.associate { details -> details.book.id.toString() to downloadOf(details, active[details.book.id].orEmpty()) }
        }.distinctUntilChanged()

    override suspend fun add(book: LibriVoxBook, allowsCellular: Boolean): DataResult<String> = withContext(Dispatchers.IO) {
        if (book.sections.isEmpty()) return@withContext DataResult.Failure(DataError.NOT_AN_AUDIOBOOK)
        dao.getBookByLibriVoxId(book.id)?.let { existing ->
            if (existing.hidden) dao.updateBook(existing.copy(hidden = false))
            control.start(existing.id, allowsCellular)
            return@withContext DataResult.Success(existing.id.toString())
        }
        val sidecarId = UUID.randomUUID().toString()
        val coverUri = catalog.coverBytes(book)?.let { bytes -> (contentSource.saveCover(sidecarId, bytes) as? DataResult.Success)?.value }
        val now = System.currentTimeMillis()
        val entity = BookEntity(
            rootId = null,
            relativePath = RELATIVE_PATH_PREFIX + book.id,
            folderUri = DETAILS_PAGE + book.id,
            folderDocumentId = book.id,
            title = book.title,
            author = book.authors.takeIf(String::isNotBlank),
            narrator = null,
            description = book.description.takeIf(String::isNotBlank),
            coverUri = coverUri,
            coverSource = coverUri?.let { COVER_SOURCE },
            durationMs = book.durationMs,
            trackCount = book.sections.size,
            isSingleFile = false,
            addedAt = now,
            lastScannedAt = now,
            folderLastModified = 0L,
            metadataComplete = true,
            sidecarId = sidecarId,
            librivoxId = book.id,
            librivoxDownload = LibriVoxDownloadMode.ACTIVE.name,
        )
        val bookId = dao.insertBookWithTracks(entity, tracksOf(book))
        scheduler.enqueue(bookId, allowsCellular, replace = false)
        DataResult.Success(bookId.toString())
    }

    override suspend fun startDownload(bookId: String, allowsCellular: Boolean) {
        bookId.toLongOrNull()?.let { control.start(it, allowsCellular) }
    }

    override suspend fun pauseDownload(bookId: String) {
        bookId.toLongOrNull()?.let { control.pause(it) }
    }

    override suspend fun removeDownload(bookId: String) {
        bookId.toLongOrNull()?.let { control.remove(it) }
    }

    override fun networkStatus(): NetworkStatus {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return NetworkStatus(isOnline = true, isMetered = false)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        return NetworkStatus(isOnline = isOnline, isMetered = isOnline && manager.isActiveNetworkMetered)
    }

    private fun tracksOf(book: LibriVoxBook): List<TrackEntity> {
        var start = 0L
        return book.sections.mapIndexed { index, section ->
            TrackEntity(
                bookId = 0L,
                indexInBook = index,
                documentUri = section.listenUrl,
                fileName = section.listenUrl.substringAfterLast('/'),
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

    private fun downloadOf(details: BookWithDetails, active: Map<Long, ChapterDownload>): LibriVoxDownload {
        val tracks = details.tracks.sortedBy { it.indexInBook }
        val chapters = tracks.map { track ->
            when {
                LibriVoxFiles.isLocal(track.documentUri) -> ChapterDownload(ChapterDownloadStatus.DOWNLOADED, 1f)
                else -> active[track.id] ?: ChapterDownload(ChapterDownloadStatus.WAITING)
            }
        }
        return LibriVoxDownload.of(chapters, tracks.map { it.sizeBytes }, LibriVoxDownloadControl.modeOf(details.book.librivoxDownload))
    }

    private suspend fun <T> guarded(block: suspend () -> T): DataResult<T> = try {
        DataResult.Success(withContext(Dispatchers.IO) { block() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        DataResult.Failure(errorOf(error))
    }

    private fun errorOf(error: Throwable): DataError = when (error) {
        is LibriVoxException -> error.error
        is IOException -> DataError.NETWORK
        else -> DataError.UNREADABLE
    }

    companion object {
        const val RELATIVE_PATH_PREFIX = "librivox:"
        private const val DETAILS_PAGE = "https://archive.org/details/"
        private const val COVER_SOURCE = "librivox"
    }
}
