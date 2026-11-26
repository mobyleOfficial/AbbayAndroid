package com.mobyle.abbay.data.repository

import android.os.SystemClock
import android.util.Log
import com.library.NaturalOrderComparator
import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.LibraryRootEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.effects.EffectDefaults
import com.mobyle.abbay.data.library.BookDetailsMapper
import com.mobyle.abbay.data.library.BookLocation
import com.mobyle.abbay.data.library.BookMetadataEnricher
import com.mobyle.abbay.data.library.BookProjection
import com.mobyle.abbay.data.library.MissingBookRetention
import com.mobyle.abbay.data.library.ScannedBookWriter
import com.mobyle.abbay.data.librivox.LibriVoxDownloadControl
import com.mobyle.abbay.data.librivox.LibriVoxDownloadScheduler
import com.mobyle.abbay.data.librivox.LibriVoxDownloadTracker
import com.mobyle.abbay.data.librivox.LibriVoxFiles
import com.mobyle.abbay.data.scanner.FileKinds
import com.mobyle.abbay.data.scanner.LibraryScanner
import com.mobyle.abbay.data.scanner.ScannedBook
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.mobyle.abbay.data.sidecar.SidecarCodec
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.mobyle.abbay.data.storage.CoverImageWriter
import com.mobyle.abbay.data.storage.DocumentContentDataSource
import com.mobyle.abbay.data.storage.DocumentEntry
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.mobyle.abbay.data.storage.TrackMetadataDataSource
import com.mobyle.abbay.data.work.ScanWorkScheduler
import com.model.Book
import com.model.BookDetails
import com.model.BookFile
import com.model.BookNameSource
import com.model.BookOverrides
import com.model.BookState
import com.model.BookType
import com.model.Chapter
import com.model.CompanionDocument
import com.model.CoverCandidate
import com.model.DataError
import com.model.DataResult
import com.model.LibraryRoot
import com.model.ScanStatus
import com.repository.LibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val dao: LibraryDao,
    private val scanner: LibraryScanner,
    private val tree: DocumentTreeSource,
    private val contentSource: DocumentContentDataSource,
    private val writer: ScannedBookWriter,
    private val enricher: BookMetadataEnricher,
    private val settingsSource: SettingsDataSource,
    private val scanWorkScheduler: ScanWorkScheduler,
    private val coverImageWriter: CoverImageWriter,
    private val metadataSource: TrackMetadataDataSource,
    private val sidecarSync: SidecarSync,
    private val effectDefaults: EffectDefaults,
    private val libriVoxFiles: LibriVoxFiles,
    private val libriVoxDownloads: LibriVoxDownloadScheduler,
    private val libriVoxTracker: LibriVoxDownloadTracker,
    private val libriVoxControl: LibriVoxDownloadControl,
) : LibraryRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val scanMutex = Mutex()
    private val listeningStateMutex = Mutex()
    private val scanStatus = MutableStateFlow(ScanStatus())

    private val metadataReaders = Semaphore(PARALLEL_METADATA_READERS)

    private val listingPermits = Semaphore(PARALLEL_FOLDER_LISTINGS)

    override fun observeRoots(): Flow<List<LibraryRoot>> = dao.observeRoots().map { roots -> roots.map { it.toDomain() } }

    override fun observeBooks(): Flow<List<Book>> = projectBooks(dao.observeVisibleBooks())

    override fun observeHiddenBooks(): Flow<List<Book>> = projectBooks(dao.observeHiddenBooks())

    private fun projectBooks(booksFlow: Flow<List<BookWithDetails>>): Flow<List<Book>> = combine(
        booksFlow,
        dao.observeRoots(),
        settingsSource.observeSettings().map { it.defaultSpeed to it.bookNameFromFolder }.distinctUntilChanged(),
    ) { books, roots, (defaultSpeed, nameFromFolder) ->
        val accessibleRootIds = roots.filter { it.isAccessible }.map { it.id }.toSet()
        books.mapNotNull { details ->
            val rootId = details.book.rootId
            BookProjection.toBook(
                details,
                isRootAccessible = rootId == null || rootId in accessibleRootIds,
                defaultSpeed = defaultSpeed,
                nameFromFolder = nameFromFolder,
            )
        }
    }

    override fun observeScanStatus(): Flow<ScanStatus> = scanStatus.asStateFlow()

    override suspend fun addRoot(treeUri: String): DataResult<LibraryRoot> = withContext(ioDispatcher) {
        val permission = contentSource.takePersistablePermission(treeUri)
        if (permission is DataResult.Failure) return@withContext permission

        when (val rootEntry = tree.rootEntry(treeUri)) {
            is DataResult.Failure -> rootEntry
            is DataResult.Success -> {
                val existingRoot = dao.getRootByUri(treeUri)
                val rootId = if (existingRoot != null) {
                    dao.setRootAccessible(existingRoot.id, true)
                    existingRoot.id
                } else {
                    dao.insertRoot(
                        LibraryRootEntity(
                            treeUri = treeUri,
                            displayName = rootEntry.value.name,
                            addedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                DataResult.Success(LibraryRoot(rootId, treeUri, rootEntry.value.name, isAccessible = true))
            }
        }
    }

    override suspend fun removeRoot(rootId: Long) = withContext(ioDispatcher) {
        val root = dao.getRoots().firstOrNull { it.id == rootId } ?: return@withContext
        dao.getBooksOfRoot(rootId).forEach { contentSource.deleteCover(it.sidecarId) }
        dao.deleteRoot(rootId)
        contentSource.releasePersistablePermission(root.treeUri)
    }

    override suspend fun addSingleFile(documentUri: String): DataResult<Unit> = withContext(ioDispatcher) {
        val permission = contentSource.takePersistablePermission(documentUri)
        if (permission is DataResult.Failure) return@withContext permission

        when (val description = contentSource.describe(documentUri)) {
            is DataResult.Failure -> description
            is DataResult.Success -> {
                val scannedBook = singleFileBook(description.value)
                val existingBook = dao.getSingleFileBooks().firstOrNull { it.relativePath == documentUri }
                val location = BookLocation(rootId = null, folderUri = documentUri, documentUriOf = { documentUri })
                val result = writer.write(location, scannedBook, existingBook, System.currentTimeMillis())
                if (existingBook?.hidden == true) dao.updateBook(existingBook.copy(hidden = false))
                if (result.needsMetadata) enricher.enrich(result.bookId, scannedBook) { documentUri }
                DataResult.Success(Unit)
            }
        }
    }

    override suspend fun describeExternalFile(documentUri: String): Book? = withContext(ioDispatcher) {
        val description = (contentSource.describe(documentUri) as? DataResult.Success)?.value ?: return@withContext null
        val metadata = (metadataSource.read(documentUri) as? DataResult.Success)?.value
        val defaultSpeed = settingsSource.observeSettings().first().defaultSpeed
        BookFile(
            id = Book.EXTERNAL_ID_PREFIX + documentUri,
            name = metadata?.title ?: FileKinds.baseName(description.name),
            thumbnail = null,
            progress = 0L,
            duration = metadata?.durationMs ?: 0L,
            speed = defaultSpeed,
            type = BookType.FILE,
            fileName = description.name,
            uri = documentUri,
            author = metadata?.artist,
            chapters = metadata?.durationMs?.takeIf { it > 0 }?.let { duration -> listOf(Chapter(0, "", 0L, duration)) }.orEmpty(),
        )
    }

    override suspend fun validateRoots() = withContext(ioDispatcher) {
        dao.getRoots().forEach { root ->
            val isAccessible = contentSource.hasPersistedReadPermission(root.treeUri) &&
                tree.rootEntry(root.treeUri) is DataResult.Success
            if (isAccessible != root.isAccessible) dao.setRootAccessible(root.id, isAccessible)
        }
    }

    override suspend fun quickScan() = scan(force = false)

    override fun requestFullScan() = scanWorkScheduler.enqueueFullScan()

    override suspend fun fullScan() = scan(force = true)

    override suspend fun hideBook(bookId: String) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        val libriVoxBook = dao.getBook(id)?.takeIf { it.librivoxId != null }
        if (libriVoxBook == null) {
            dao.hideBook(id)
            return@withContext
        }
        libriVoxDownloads.cancel(id)
        libriVoxTracker.clear(id)
        dao.deleteBooks(listOf(id))
        libriVoxBook.librivoxId?.let(libriVoxFiles::delete)
        contentSource.deleteCover(libriVoxBook.sidecarId)
    }

    override suspend fun restoreBook(bookId: String) = withContext(ioDispatcher) {
        bookId.toLongOrNull()?.let { dao.restoreBook(it) } ?: Unit
    }

    override suspend fun clearLibrary() = withContext(ioDispatcher) {
        val roots = dao.getRoots()
        libriVoxDownloads.cancelAll()
        libriVoxTracker.clearAll()
        dao.getAllBooks().forEach { contentSource.deleteCover(it.sidecarId) }
        dao.getSingleFileBooks().forEach { contentSource.releasePersistablePermission(it.relativePath) }
        dao.clearLibrary()
        libriVoxFiles.deleteAll()
        roots.forEach { contentSource.releasePersistablePermission(it.treeUri) }
    }

    override suspend fun saveListeningState(bookId: String, trackIndex: Int, positionInTrackMs: Long, speed: Float, pausedAt: Long?) =
        withContext(ioDispatcher) {
            listeningStateMutex.withLock { writeListeningState(bookId, trackIndex, positionInTrackMs, speed, pausedAt) }
        }

    private suspend fun writeListeningState(bookId: String, trackIndex: Int, positionInTrackMs: Long, speed: Float, pausedAt: Long?) {
        val id = bookId.toLongOrNull() ?: return
        val details = dao.getBookWithDetails(id) ?: return
        val tracks = BookProjection.playableTracks(details)
        val current = details.listeningState ?: effectDefaults.newListeningState(id)
        val startOfTrack = tracks.getOrNull(trackIndex)?.startInBookMs ?: 0L
        val hasStarted = trackIndex > 0 || positionInTrackMs > 0L
        val now = System.currentTimeMillis()
        val state = when {
            current.state == BookState.FINISHED.name -> current.state
            hasStarted -> BookState.IN_PROGRESS.name
            else -> current.state
        }

        dao.upsertListeningState(
            current.copy(
                trackIndex = trackIndex,
                positionInTrackMs = positionInTrackMs,
                bookPositionMs = startOfTrack + positionInTrackMs,
                state = state,
                speed = speed,
                lastPlayedAt = if (hasStarted) now else current.lastPlayedAt,
                lastPausedAt = pausedAt ?: current.lastPausedAt,
                updatedAt = now,
            ),
        )
        sidecarSync.onPositionSaved(id, isPause = pausedAt != null)
    }

    override suspend fun setBookState(bookId: String, state: BookState) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        val current = dao.getListeningState(id) ?: ListeningStateEntity(bookId = id)
        val updated = if (state == BookState.NEW) {
            current.copy(trackIndex = 0, positionInTrackMs = 0L, bookPositionMs = 0L, state = state.name)
        } else {
            current.copy(state = state.name)
        }
        dao.upsertListeningState(updated.copy(updatedAt = System.currentTimeMillis()))
        sidecarSync.onBookChanged(id)
    }

    override fun observeBookDetails(bookId: String): Flow<BookDetails?> {
        val id = bookId.toLongOrNull() ?: return flowOf(null)
        return combine(dao.observeBookWithDetails(id), observeNameFromFolder()) { details, nameFromFolder ->
            details?.let { BookDetailsMapper.toDetails(it, nameFromFolder) }
        }
    }

    override suspend fun updateOverrides(bookId: String, overrides: BookOverrides) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        dao.updateOverrides(
            bookId = id,
            title = overrides.title?.trim()?.takeIf(String::isNotEmpty),
            author = overrides.author?.trim()?.takeIf(String::isNotEmpty),
            narrator = overrides.narrator?.trim()?.takeIf(String::isNotEmpty),
            description = overrides.description?.trim()?.takeIf(String::isNotEmpty),
        )
        sidecarSync.onBookEdited(id)
    }

    override suspend fun setSortByTrackNumber(bookId: String, enabled: Boolean) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        val details = dao.getBookWithDetails(id) ?: return@withContext
        val current = details.listeningState ?: ListeningStateEntity(bookId = id)
        val currentFile = BookProjection.playableTracks(details).getOrNull(current.trackIndex)?.documentUri
        val reordered = BookProjection.playableTracks(details.copy(listeningState = current.copy(sortByTrackTag = enabled)))
        val newIndex = reordered.indexOfFirst { it.documentUri == currentFile }.coerceAtLeast(0)
        dao.upsertListeningState(current.copy(sortByTrackTag = enabled, trackIndex = newIndex, updatedAt = System.currentTimeMillis()))
        sidecarSync.onBookEdited(id)
    }

    override suspend fun setBookNameSource(bookId: String, source: BookNameSource) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        dao.updateNameSource(id, source.name.takeUnless { source == BookNameSource.APP_DEFAULT })
        sidecarSync.onBookEdited(id)
    }

    private fun observeNameFromFolder(): Flow<Boolean> =
        settingsSource.observeSettings().map { it.bookNameFromFolder }.distinctUntilChanged()

    override suspend fun coverCandidates(bookId: String): List<CoverCandidate> = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext emptyList()
        val details = dao.getBookWithDetails(id) ?: return@withContext emptyList()
        val root = details.book.rootId?.let { rootId -> dao.getRoots().firstOrNull { it.id == rootId } }
        val folderImages = root?.let { libraryRoot ->
            (tree.listChildren(libraryRoot.treeUri, details.book.folderDocumentId) as? DataResult.Success)?.value.orEmpty()
                .filter { FileKinds.isImage(it) && !FileKinds.isIgnored(it) }
                .map { CoverCandidate(uri = tree.documentUri(libraryRoot.treeUri, it.documentId), name = it.name, isEmbedded = false) }
        }.orEmpty()
        val embedded = BookProjection.playableTracks(details).take(EMBEDDED_COVER_TRACKS).firstNotNullOfOrNull { track ->
            (metadataSource.readEmbeddedPicture(track.documentUri) as? DataResult.Success)?.value
                ?.takeIf { it.isNotEmpty() }
                ?.let { picture -> (contentSource.saveCover("embedded_${details.book.sidecarId}", picture) as? DataResult.Success)?.value }
                ?.let { uri -> CoverCandidate(uri = uri, name = track.fileName, isEmbedded = true) }
        }
        listOfNotNull(embedded) + folderImages
    }

    override suspend fun companionDocuments(bookId: String): List<CompanionDocument> = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext emptyList()
        val details = dao.getBookWithDetails(id) ?: return@withContext emptyList()
        if (details.book.isSingleFile) return@withContext emptyList()
        val root = details.book.rootId?.let { rootId -> dao.getRoots().firstOrNull { it.id == rootId && it.isAccessible } }
            ?: return@withContext emptyList()
        fun children(folderId: String) =
            (tree.listChildren(root.treeUri, folderId) as? DataResult.Success)?.value.orEmpty().filterNot(FileKinds::isIgnored)
        val top = children(details.book.folderDocumentId)
        val found = top.filter(FileKinds::isCompanionDocument) +
            top.filter { it.isDirectory }.flatMap { folder -> children(folder.documentId).filter(FileKinds::isCompanionDocument) }
        val lastOpened = details.listeningState?.lastOpenedDocument
        val nameOrder = NaturalOrderComparator()
        found.map { entry ->
            val uri = tree.documentUri(root.treeUri, entry.documentId)
            CompanionDocument(name = entry.name, uri = uri, isLastOpened = uri == lastOpened)
        }.sortedWith(compareByDescending<CompanionDocument> { it.isLastOpened }.thenBy(nameOrder) { it.name })
    }

    override suspend fun rememberOpenedDocument(bookId: String, documentUri: String) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        val state = dao.getListeningState(id) ?: ListeningStateEntity(bookId = id)
        dao.upsertListeningState(state.copy(lastOpenedDocument = documentUri))
    }

    override suspend fun setUserCover(bookId: String, imageUri: String): DataResult<Unit> = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext DataResult.Failure(DataError.UNREADABLE)
        val book = dao.getBook(id) ?: return@withContext DataResult.Failure(DataError.UNREADABLE)
        val jpeg = when (val image = coverImageWriter.readAsJpeg(imageUri)) {
            is DataResult.Failure -> return@withContext image
            is DataResult.Success -> image.value
        }
        val root = book.rootId?.let { rootId -> dao.getRoots().firstOrNull { it.id == rootId } }
        val inFolder = root?.let {
            val children = (tree.listChildren(it.treeUri, book.folderDocumentId) as? DataResult.Success)?.value.orEmpty()
            coverImageWriter.writeIntoBookFolder(it.treeUri, book.folderDocumentId, children, jpeg) as? DataResult.Success
        }?.value
        val coverUri = inFolder ?: when (val saved = contentSource.saveCover("user_${book.sidecarId}", jpeg)) {
            is DataResult.Failure -> return@withContext saved
            is DataResult.Success -> saved.value
        }
        dao.updateCover(id, coverUri, BookMetadataEnricher.COVER_SOURCE_USER)
        DataResult.Success(Unit)
    }

    private suspend fun scan(force: Boolean) = withContext(ioDispatcher) {
        scanMutex.withLock {
            val existingBookCount = dao.countBooks()
            val startedAt = SystemClock.elapsedRealtime()
            scanStatus.value = ScanStatus(isScanning = true, processedBooks = 0, totalBooks = existingBookCount)
            try {
                dao.getRoots().filter { it.isAccessible }.forEach { root -> scanRoot(root, force) }
                checkSingleFileBooks()
                libriVoxControl.repairAll()
                deleteBooksMissingForTooLong()
            } finally {
                val processedBooks = scanStatus.value.processedBooks
                scanStatus.value = ScanStatus()
                val elapsedMs = SystemClock.elapsedRealtime() - startedAt
                Log.i(LOG_TAG, "${if (force) "full" else "quick"} scan: $processedBooks books in $elapsedMs ms")
            }
        }
    }

    private suspend fun scanRoot(root: LibraryRootEntity, force: Boolean) {
        val rootChildren = when (val listing = scanner.listRoot(root.treeUri)) {
            is DataResult.Success -> listing.value
            is DataResult.Failure -> {
                if (listing.error == DataError.ACCESS_LOST) dao.setRootAccessible(root.id, false)
                return
            }
        }
        val existingBooks = dao.getBooksOfRoot(root.id).associateBy { it.relativePath }
        val pass = ScanPass(root, existingBooks, force)

        scanner.looseFileBooks(rootChildren).forEach { looseBook ->
            val existingBook = existingBooks[looseBook.relativePath]
            if (pass.isUnchanged(existingBook, looseBook.folderLastModified)) {
                pass.markSeen(looseBook.relativePath)
            } else {
                pass.store(looseBook)
            }
        }
        coroutineScope {
            rootChildren.filter { it.isDirectory }
                .map { folder -> async { scanFolder(pass, folder, folder.name, parentFolderName = null) } }
                .awaitAll()
        }

        coroutineScope {
            pass.booksNeedingMetadata.toList().map { (bookId, scannedBook) ->
                async {
                    metadataReaders.withPermit {
                        enricher.enrich(bookId, scannedBook) { documentId -> tree.documentUri(root.treeUri, documentId) }
                    }
                    advanceProgress()
                }
            }.awaitAll()
        }
        markMissingBooks(existingBooks.values, pass.seenPaths)
    }

    private suspend fun scanFolder(pass: ScanPass, folder: DocumentEntry, relativePath: String, parentFolderName: String?) {
        if (pass.isUnchanged(pass.existingBooks[relativePath], folder.lastModified)) {
            pass.markSeen(relativePath)
            pass.existingBooks.keys.filter { it.startsWith("$relativePath/") }.forEach(pass::markSeen)
            advanceProgress()
            return
        }

        val listing = listingPermits.withPermit { tree.listChildren(pass.root.treeUri, folder.documentId) }
        val children = (listing as? DataResult.Success)?.value ?: return

        if (scanner.isBookFolder(children)) {
            val books = listingPermits.withPermit {
                scanner.scanFolder(pass.root.treeUri, folder, relativePath, parentFolderName, children)
            }
            books.forEach { pass.store(it) }
        } else {
            coroutineScope {
                children.filter { it.isDirectory && !FileKinds.isIgnored(it) }
                    .map { subfolder -> async { scanFolder(pass, subfolder, "$relativePath/${subfolder.name}", folder.name) } }
                    .awaitAll()
            }
        }
    }

    private suspend fun ScanPass.store(scannedBook: ScannedBook) {
        val location = BookLocation(
            rootId = root.id,
            folderUri = tree.documentUri(root.treeUri, scannedBook.folderDocumentId),
            documentUriOf = { documentId -> tree.documentUri(root.treeUri, documentId) },
        )
        val sidecar = scannedBook.sidecarFolderDocumentId?.let { sidecarSync.read(root.treeUri, it) }
        val existingBook = existingBooks[scannedBook.relativePath] ?: sidecar?.let { reattachableBook(it) }
        val result = writer.write(location, scannedBook, existingBook, System.currentTimeMillis())
        existingBook?.relativePath?.let(::markSeen)
        markSeen(scannedBook.relativePath)
        sidecar?.let { sidecarSync.restore(result.bookId, it) }
        if (result.needsMetadata) booksNeedingMetadata += result.bookId to scannedBook else advanceProgress()
    }

    private suspend fun reattachableBook(sidecar: SidecarCodec.Read): BookEntity? {
        val book = dao.getBookBySidecarId(sidecar.book.id)?.takeUnless { it.isSingleFile } ?: return null
        return book.takeIf { contentSource.describe(book.folderUri) is DataResult.Failure }
    }

    private suspend fun markMissingBooks(books: Collection<BookEntity>, seenPaths: Set<String>) {
        val now = System.currentTimeMillis()
        books.forEach { book ->
            val isSeen = book.relativePath in seenPaths
            when {
                !isSeen && book.missingSince == null -> dao.setMissingSince(book.id, now)
                isSeen && book.missingSince != null -> dao.setMissingSince(book.id, null)
            }
        }
    }

    private suspend fun checkSingleFileBooks() {
        val now = System.currentTimeMillis()
        dao.getSingleFileBooks().forEach { book ->
            val isPresent = contentSource.describe(book.relativePath) is DataResult.Success
            when {
                !isPresent && book.missingSince == null -> dao.setMissingSince(book.id, now)
                isPresent && book.missingSince != null -> dao.setMissingSince(book.id, null)
            }
        }
    }

    private suspend fun deleteBooksMissingForTooLong() {
        val retentionDays = settingsSource.observeSettings().first().missingBookRetentionDays
        val removesAtOnce = MissingBookRetention.removesAtOnce(retentionDays)
        val expiredBooks = dao.getBooksMissingSince(MissingBookRetention.cutoff(retentionDays, System.currentTimeMillis()))
            // With no time to come back, a folder that failed to list once must not cost the book: it goes only
            // when its files are really gone.
            .filter { book -> !removesAtOnce || hasNoFilesLeft(book) }
        expiredBooks.forEach { contentSource.deleteCover(it.sidecarId) }
        if (expiredBooks.isNotEmpty()) dao.deleteBooks(expiredBooks.map { it.id })
    }

    /** Its folder is gone, or it is still there with none of the book's audio in it (the files went to the trash). */
    private suspend fun hasNoFilesLeft(book: BookEntity): Boolean =
        contentSource.describe(book.folderUri) is DataResult.Failure ||
            dao.getTracks(book.id).none { track -> contentSource.describe(track.documentUri) is DataResult.Success }

    private fun advanceProgress() {
        scanStatus.update { status ->
            val processed = status.processedBooks + 1
            status.copy(processedBooks = processed, totalBooks = maxOf(status.totalBooks, processed))
        }
    }

    private fun singleFileBook(file: DocumentEntry) = ScannedBook(
        relativePath = file.documentId,
        folderDocumentId = file.documentId,
        folderName = FileKinds.baseName(file.name),
        folderLastModified = file.lastModified,
        parentFolderName = null,
        isSingleFile = true,
        tracks = listOf(file),
        unsupportedFiles = emptyList(),
        images = emptyList(),
        descriptionFile = null,
    )

    private fun LibraryRootEntity.toDomain() = LibraryRoot(id, treeUri, displayName, isAccessible)

    private class ScanPass(val root: LibraryRootEntity, val existingBooks: Map<String, BookEntity>, private val force: Boolean) {
        val seenPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()
        val booksNeedingMetadata: MutableList<Pair<Long, ScannedBook>> = Collections.synchronizedList(mutableListOf())

        fun markSeen(relativePath: String) {
            seenPaths += relativePath
        }

        fun isUnchanged(existingBook: BookEntity?, lastModified: Long): Boolean =
            !force && existingBook != null && existingBook.metadataComplete && existingBook.folderLastModified == lastModified
    }

    private companion object {
        const val LOG_TAG = "LibraryScan"
        const val PARALLEL_METADATA_READERS = 4
        const val PARALLEL_FOLDER_LISTINGS = 6
        const val EMBEDDED_COVER_TRACKS = 3
    }
}
