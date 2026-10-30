package com.mobyle.abbay.data.librivox

import com.mobyle.abbay.data.database.dao.LibraryDao
import com.model.LibriVoxDownloadMode
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
        scheduler.enqueue(bookId, allowsCellular, replace = true)
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

    companion object {
        fun modeOf(stored: String?): LibriVoxDownloadMode =
            LibriVoxDownloadMode.entries.firstOrNull { it.name == stored } ?: LibriVoxDownloadMode.ACTIVE
    }
}
