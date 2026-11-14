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

@Suppress("TooManyFunctions")
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

    private val rawByBook = ConcurrentHashMap<Long, JsonObject>()
    private val writeMutex = Mutex()

    fun onPositionSaved(bookId: Long, isPause: Boolean, now: Long = System.currentTimeMillis()) {
        val last = lastWriteAt[bookId] ?: 0L
        if (isPause || now - last >= PERIODIC_INTERVAL_MS) requestWrite(bookId, now)
    }

    fun onBookChanged(bookId: Long, now: Long = System.currentTimeMillis()) = requestWrite(bookId, now)

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

    fun read(treeUri: String, sidecarFolderDocumentId: String): SidecarCodec.Read? =
        (files.read(treeUri, sidecarFolderDocumentId) as? DataResult.Success)?.value?.let(SidecarCodec::read)

    suspend fun restore(bookId: Long, read: SidecarCodec.Read): Boolean {
        rawByBook[bookId] = read.raw
        if (dao.getBookBySidecarId(read.book.id) == null) dao.updateSidecarId(bookId, read.book.id)
        return apply(bookId, read.book)
    }

    suspend fun apply(bookId: Long, sidecar: SidecarBook): Boolean {
        val details = dao.getBookWithDetails(bookId) ?: return false
        if (!SidecarMapper.isNewer(sidecar, details)) return false
        dao.upsertListeningState(SidecarMapper.toListeningState(sidecar, details))
        listeningDao.replaceBookmarks(bookId, SidecarMapper.toBookmarks(sidecar, bookId))
        extrasDao.replaceCharacters(bookId, SidecarMapper.toCharacters(sidecar, bookId))
        with(sidecar.overrides) { dao.updateOverrides(bookId, title, author, narrator, description) }
        return true
    }

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
