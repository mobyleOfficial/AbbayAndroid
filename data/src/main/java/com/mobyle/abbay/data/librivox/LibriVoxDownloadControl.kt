package com.mobyle.abbay.data.librivox

import androidx.work.ExistingWorkPolicy
import com.librivox.CellularDownloadPolicy
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.model.LibriVoxDownloadMode
import com.model.NetworkStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibriVoxDownloadControl @Inject constructor(
    private val dao: LibraryDao,
    private val scheduler: LibriVoxDownloadScheduler,
    private val tracker: LibriVoxDownloadTracker,
    private val files: LibriVoxFiles,
) {
    suspend fun start(bookId: Long, allowsCellular: Boolean) {
        dao.setLibriVoxDownload(bookId, LibriVoxDownloadMode.ACTIVE.name)
        tracker.clear(bookId)
        scheduler.enqueue(bookId, allowsCellular, ExistingWorkPolicy.REPLACE)
    }

    suspend fun pause(bookId: Long) {
        dao.setLibriVoxDownload(bookId, LibriVoxDownloadMode.PAUSED.name)
        scheduler.cancel(bookId)
        tracker.clear(bookId)
    }

    suspend fun remove(bookId: Long) {
        val book = dao.getBook(bookId) ?: return
        dao.setLibriVoxDownload(bookId, LibriVoxDownloadMode.OFF.name)
        scheduler.cancel(bookId)
        tracker.clear(bookId)
        dao.resetTracksToRemote(bookId)
        book.librivoxId?.let(files::delete)
    }

    suspend fun repairAll() {
        dao.getLibriVoxBooks().forEach { book -> repair(book.id) }
    }

    suspend fun repair(bookId: Long): Boolean {
        val book = dao.getBook(bookId)?.takeIf { it.librivoxId != null } ?: return false
        val repaired = files.restoreMissingFiles(bookId)
        val tracks = dao.getTracks(bookId)
        val isIncomplete = tracks.any { !LibriVoxFiles.isLocal(it.documentUri) }
        if (isIncomplete && modeOf(book.librivoxDownload) == LibriVoxDownloadMode.ACTIVE) {
            val decision = CellularDownloadPolicy.decide(tracks.sumOf { it.sizeBytes }, NetworkStatus(isOnline = true, isMetered = false))
            val allowsCellular = (decision as? CellularDownloadPolicy.Decision.Proceed)?.allowsCellular ?: false
            scheduler.enqueue(bookId, allowsCellular, if (repaired) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP)
        }
        return repaired
    }

    companion object {
        fun modeOf(stored: String?): LibriVoxDownloadMode =
            LibriVoxDownloadMode.entries.firstOrNull { it.name == stored } ?: LibriVoxDownloadMode.ACTIVE
    }
}
