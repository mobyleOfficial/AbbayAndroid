package com.mobyle.abbay.data.sidecar

import com.mobyle.abbay.data.database.dao.ExtrasDao
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.dao.ListeningDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.model.DataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps `.abbay/book.json` in step with the database: writes on pause, at most every
 * 60 s while playing and on bookmark, character or metadata changes, never more often than once per 10 s per book; on scan,
 * restores the newer state. One-file books that share a folder with other books have no sidecar.
 */
@Suppress("TooManyFunctions") // The sidecar write triggers plus restore and snapshot.
@Singleton
class SidecarSync @Inject constructor(
    private val dao: LibraryDao,
    private val listeningDao: ListeningDao,
    private val extrasDao: ExtrasDao,
    private val files: SidecarFileSource,
    private val settings: SettingsDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastWriteAt = ConcurrentHashMap<Long, Long>()
    private val pendingWrites = ConcurrentHashMap<Long, Job>()

    /** The JSON last read or written per book, so unknown fields survive the next write. */
    private val rawByBook = ConcurrentHashMap<Long, JsonObject>()
    private val writeMutex = Mutex()

    /** A position save: a pause writes now (throttled); periodic saves write once a minute. */
    fun onPositionSaved(bookId: Long, isPause: Boolean, now: Long = System.currentTimeMillis()) {
        val last = lastWriteAt[bookId] ?: 0L
        if (isPause || now - last >= PERIODIC_INTERVAL_MS) requestWrite(bookId, now)
    }

    /** The listening state changed outside a position save (book state). */
    fun onBookChanged(bookId: Long, now: Long = System.currentTimeMillis()) = requestWrite(bookId, now)

    /**
     * Bookmarks, characters, overrides or track order changed: the state's `updatedAt` moves too, because it decides which side
     * wins between the database and a sidecar or backup.
     */
    suspend fun onBookEdited(bookId: Long, now: Long = System.currentTimeMillis()) {
        val state = dao.getListeningState(bookId) ?: ListeningStateEntity(bookId = bookId)
        dao.upsertListeningState(state.copy(updatedAt = now))
        requestWrite(bookId, now)
    }

    private fun requestWrite(bookId: Long, now: Long) {
        if (pendingWrites[bookId]?.isActive == true) return
        val waitMs = ((lastWriteAt[bookId] ?: 0L) + MINIMUM_INTERVAL_MS - now).coerceAtLeast(0L)
        pendingWrites[bookId] = scope.launch {
            delay(waitMs)
            write(bookId)
        }
    }

    /** Writes the sidecar of [bookId] now, when the setting allows it and the book has a folder of its own. */
    suspend fun write(bookId: Long) {
        if (!settings.observeSettings().first().saveProgressInFolders) return
        val details = dao.getBookWithDetails(bookId) ?: return
        val location = locationOf(details.book) ?: return
        writeMutex.withLock {
            val previous = rawByBook[bookId] ?: readRaw(location)
            val sidecar = SidecarMapper.toSidecar(details, listeningDao.getBookmarks(bookId), extrasDao.getCharacters(bookId))
            val encoded = SidecarCodec.encode(sidecar, previous)
            if (files.write(location.treeUri, location.folderDocumentId, SidecarCodec.write(sidecar, previous)) is DataResult.Success) {
                rawByBook[bookId] = encoded
                lastWriteAt[bookId] = System.currentTimeMillis()
            }
        }
    }

    /** Reads the sidecar in the `.abbay` folder [sidecarFolderDocumentId] of a scanned book. */
    fun read(treeUri: String, sidecarFolderDocumentId: String): SidecarCodec.Read? =
        (files.read(treeUri, sidecarFolderDocumentId) as? DataResult.Success)?.value?.let(SidecarCodec::read)

    /**
     * Applies [read] to [bookId] when it is newer than the database and adopts its id when no other book has it,
     * so a renamed folder can be re-attached later. Returns true when the state was restored.
     */
    suspend fun restore(bookId: Long, read: SidecarCodec.Read): Boolean {
        rawByBook[bookId] = read.raw
        if (dao.getBookBySidecarId(read.book.id) == null) dao.updateSidecarId(bookId, read.book.id)
        return apply(bookId, read.book)
    }

    /** Applies [sidecar] to [bookId] when it is newer; used by the scan and by backup import. */
    suspend fun apply(bookId: Long, sidecar: SidecarBook): Boolean {
        val details = dao.getBookWithDetails(bookId) ?: return false
        if (!SidecarMapper.isNewer(sidecar, details)) return false
        dao.upsertListeningState(SidecarMapper.toListeningState(sidecar, details))
        listeningDao.replaceBookmarks(bookId, SidecarMapper.toBookmarks(sidecar, bookId))
        extrasDao.replaceCharacters(bookId, SidecarMapper.toCharacters(sidecar, bookId))
        with(sidecar.overrides) { dao.updateOverrides(bookId, title, author, narrator, description) }
        return true
    }

    /** The database state of [bookId] in the sidecar shape (backup export). */
    suspend fun snapshot(bookId: Long): SidecarBook? {
        val details = dao.getBookWithDetails(bookId) ?: return null
        return SidecarMapper.toSidecar(details, listeningDao.getBookmarks(bookId), extrasDao.getCharacters(bookId))
    }

    private suspend fun readRaw(location: Location): JsonObject? {
        val sidecarFolder = location.sidecarFolderId() ?: return null
        return read(location.treeUri, sidecarFolder)?.raw
    }

    private suspend fun locationOf(book: BookEntity): Location? {
        if (book.isSingleFile) return null
        val rootId = book.rootId ?: return null
        val root = dao.getRoots().firstOrNull { it.id == rootId && it.isAccessible } ?: return null
        return Location(root.treeUri, book.folderDocumentId)
    }

    private inner class Location(val treeUri: String, val folderDocumentId: String) {
        fun sidecarFolderId(): String? = files.sidecarFolderOf(treeUri, folderDocumentId)
    }

    private companion object {
        const val MINIMUM_INTERVAL_MS = 10_000L
        const val PERIODIC_INTERVAL_MS = 60_000L
    }
}
