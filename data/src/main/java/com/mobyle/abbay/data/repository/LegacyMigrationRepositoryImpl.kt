package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.database.dao.LegacyDao
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.legacy.LegacyBookMatcher
import com.mobyle.abbay.data.library.BookProjection
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.model.BookState
import com.repository.LegacyMigrationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyMigrationRepositoryImpl @Inject constructor(
    private val legacyDao: LegacyDao,
    private val libraryDao: LibraryDao,
    private val settingsSource: SettingsDataSource,
    private val contentSource: DocumentContentDataSource,
) : LegacyMigrationRepository {
    private val importMutex = Mutex()

    override suspend fun legacyFolderUri(): String? = settingsSource.legacyFolderUri()

    override suspend fun isLegacyFolderStillAccessible(): Boolean = withContext(Dispatchers.IO) {
        legacyFolderUri()?.let(contentSource::hasPersistedReadPermission) ?: false
    }

    override suspend fun markLegacyFolderMigrated() = settingsSource.markLegacyFolderMigrated()

    /**
     * Copies position, current track and speed of every legacy book that matches a scanned book which has not been
     * played yet. Matched legacy rows are deleted, so the import is safe to run after every scan; the legacy tables
     * end up empty once every old book was found again.
     */
    override suspend fun importLegacyProgress(): Int = withContext(Dispatchers.IO) {
        importMutex.withLock {
            val legacyFiles = legacyDao.getBookFiles()
            val legacyMultipleBooks = legacyDao.getMultipleBooks()
            if (legacyFiles.isEmpty() && legacyMultipleBooks.isEmpty()) return@withLock 0

            val books = libraryDao.getAllBooksWithDetails()
            val candidates = books.map { details ->
                LegacyBookMatcher.Candidate(
                    bookId = details.book.id,
                    title = details.book.title,
                    trackFileNames = BookProjection.playableTracks(details).map { it.fileName },
                )
            }
            val playedBookIds = books.filter { it.listeningState != null }.map { it.book.id }.toMutableSet()
            val legacySelectedMediaId = settingsSource.legacySelectedMediaId()
            var importedCount = 0
            val pendingStates = mutableListOf<ListeningStateEntity>()
            var selectedBookId: String? = null

            fun import(legacyId: String, legacyBook: LegacyBookMatcher.LegacyBook, progress: Long, speed: Float): Boolean {
                val match = LegacyBookMatcher.match(legacyBook, candidates) ?: return false
                if (match.bookId in playedBookIds) return true
                playedBookIds += match.bookId
                pendingStates += ListeningStateEntity(
                    bookId = match.bookId,
                    trackIndex = match.trackIndex,
                    positionInTrackMs = progress,
                    state = if (progress > 0L || match.trackIndex > 0) BookState.IN_PROGRESS.name else BookState.NEW.name,
                    speed = speed,
                    updatedAt = System.currentTimeMillis(),
                )
                if (legacyId == legacySelectedMediaId) selectedBookId = match.bookId.toString()
                importedCount++
                return true
            }

            val importedMultipleIds = legacyMultipleBooks.filter { legacy ->
                import(
                    legacyId = legacy.id,
                    legacyBook = LegacyBookMatcher.LegacyBook(
                        name = legacy.name,
                        trackFileNames = LegacyBookMatcher.decodeTrackFileNames(legacy.bookFileList),
                        currentTrackIndex = legacy.currentBookPosition,
                    ),
                    progress = legacy.progress,
                    speed = legacy.speed,
                )
            }.map { it.id }
            val importedFileIds = legacyFiles.filter { legacy ->
                import(legacy.id, LegacyBookMatcher.LegacyBook(legacy.name, listOf(legacy.fileName), 0), legacy.progress, legacy.speed)
            }.map { it.id }

            pendingStates.forEach { state ->
                val tracks = books.firstOrNull { it.book.id == state.bookId }?.let(BookProjection::playableTracks).orEmpty()
                val startOfTrack = tracks.getOrNull(state.trackIndex)?.startInBookMs ?: 0L
                libraryDao.upsertListeningState(state.copy(bookPositionMs = startOfTrack + state.positionInTrackMs))
            }
            selectedBookId?.let { bookId ->
                settingsSource.updateSettings { it.copy(lastBookId = bookId) }
                settingsSource.clearLegacySelectedMediaId()
            }
            if (importedMultipleIds.isNotEmpty()) legacyDao.deleteMultipleBooks(importedMultipleIds)
            if (importedFileIds.isNotEmpty()) legacyDao.deleteBookFiles(importedFileIds)

            importedCount
        }
    }
}
