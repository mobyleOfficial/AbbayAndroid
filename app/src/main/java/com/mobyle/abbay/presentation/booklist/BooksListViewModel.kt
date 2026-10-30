package com.mobyle.abbay.presentation.booklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.library.LibraryLayout
import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.listening.ShakeSensitivity
import com.model.Book
import com.model.BookFile
import com.model.BookState
import com.model.Bookmark
import com.model.LibraryRoot
import com.model.MultipleBooks
import com.model.ScanStatus
import com.player.PlaybackStatus
import com.player.PlayerController
import com.player.PlayerError
import com.player.PlayerState
import com.player.SleepTimerMode
import com.settings.AppSettings
import com.usecase.AddBookmark
import com.usecase.AddLibraryRoot
import com.usecase.AddSingleFileBook
import com.usecase.AddToQueue
import com.usecase.CompleteLegacyFolderMigration
import com.usecase.GetAppSettings
import com.usecase.GetLastBook
import com.usecase.GetOpusEncoder
import com.usecase.HideBook
import com.usecase.ImportLegacyProgress
import com.usecase.ObserveAppSettings
import com.usecase.ObserveBooks
import com.usecase.ObserveLibraryRoots
import com.usecase.ObserveScanStatus
import com.usecase.OpenExternalFile
import com.usecase.PrepareLegacyFolderMigration
import com.usecase.RefreshLibrary
import com.usecase.RefreshLibraryOnStart
import com.usecase.RequestFullScan
import com.usecase.SaveListeningState
import com.usecase.SetBookState
import com.usecase.SortAndFilterBooks
import com.usecase.UpdateAppSettings
import com.usecase.UpdateBookmarkNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val saveListeningState: SaveListeningState,
    private val updateAppSettings: UpdateAppSettings,
    private val addLibraryRoot: AddLibraryRoot,
    private val addSingleFileBook: AddSingleFileBook,
    private val refreshLibrary: RefreshLibrary,
    private val refreshLibraryOnStart: RefreshLibraryOnStart,
    private val requestFullScan: RequestFullScan,
    private val hideBook: HideBook,
    private val setBookState: SetBookState,
    private val sortAndFilterBooks: SortAndFilterBooks,
    private val prepareLegacyFolderMigration: PrepareLegacyFolderMigration,
    private val completeLegacyFolderMigration: CompleteLegacyFolderMigration,
    private val importLegacyProgress: ImportLegacyProgress,
    private val getAppSettings: GetAppSettings,
    private val addBookmark: AddBookmark,
    private val updateBookmarkNote: UpdateBookmarkNote,
    private val getLastBook: GetLastBook,
    private val openExternalFile: OpenExternalFile,
    observeBooks: ObserveBooks,
    observeLibraryRoots: ObserveLibraryRoots,
    observeScanStatus: ObserveScanStatus,
    observeAppSettings: ObserveAppSettings,
    private val addToQueue: AddToQueue,
    private val getOpusEncoder: GetOpusEncoder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<BooksListUiState>(BooksListUiState.Loading)
    val uiState: StateFlow<BooksListUiState> get() = _uiState

    var booksList = listOf<Book>()
        private set

    val playerState: StateFlow<PlayerState> get() = playerController.state

    val isPlaying: StateFlow<Boolean> = playerController.state
        .map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isAwaitingPlayback = MutableStateFlow(false)
    val isAwaitingPlayback: StateFlow<Boolean> get() = _isAwaitingPlayback

    private val _expandPlayerEvents = Channel<Unit>(Channel.CONFLATED)
    val expandPlayerEvents: Flow<Unit> = _expandPlayerEvents.receiveAsFlow()

    private val _externalFileEvents = Channel<Boolean>(Channel.CONFLATED)
    val externalFileEvents: Flow<Boolean> = _externalFileEvents.receiveAsFlow()

    private val _collapsePlayerEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val collapsePlayerEvents: SharedFlow<Unit> get() = _collapsePlayerEvents.asSharedFlow()

    private val _skippedTrackMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val skippedTrackMessages: SharedFlow<String> get() = _skippedTrackMessages.asSharedFlow()

    private val _playRequests = Channel<Book>(Channel.CONFLATED)
    val playRequests: Flow<Book> = _playRequests.receiveAsFlow()

    private val _queueAdded = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val queueAdded: SharedFlow<Boolean> = _queueAdded.asSharedFlow()

    private val _noiseReductionTurnedOff = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val noiseReductionTurnedOff: SharedFlow<Unit> = _noiseReductionTurnedOff.asSharedFlow()

    private val _bookmarkAdded = MutableSharedFlow<Bookmark>(extraBufferCapacity = 1)
    val bookmarkAdded: SharedFlow<Bookmark> get() = _bookmarkAdded.asSharedFlow()

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> get() = _selectedBook

    private val _currentProgress = MutableStateFlow(0L)
    val currentProgress: StateFlow<Long> get() = _currentProgress

    val hasBookSelected = _selectedBook.map { it != null }

    private val _shouldOpenPlayerInStartup = MutableStateFlow(false)
    val shouldOpenPlayerInStartup: StateFlow<Boolean> get() = _shouldOpenPlayerInStartup

    val isScreenLocked = MutableStateFlow(false)

    private val settings: StateFlow<AppSettings> = observeAppSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val appSettings: StateFlow<AppSettings> get() = settings

    private val roots: StateFlow<List<LibraryRoot>> = observeLibraryRoots()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val scanStatus: StateFlow<ScanStatus> = observeScanStatus()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ScanStatus())

    val hasSelectedFolder: StateFlow<Boolean> = roots.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isRefreshing: StateFlow<Boolean> = scanStatus.map { it.isScanning }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val selectedTab = MutableStateFlow<LibraryTab?>(null)

    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState: StateFlow<LibraryUiState> get() = _libraryState

    private val _showReloadGuide = MutableStateFlow(false)
    val showReloadGuide: StateFlow<Boolean> get() = _showReloadGuide

    private val _showBookEndedDialog = MutableStateFlow(false)
    val showBookEndedDialog: StateFlow<Boolean> get() = _showBookEndedDialog

    private val failedBookIds = MutableStateFlow(emptySet<String>())
    private val legacyFolderToReselect = MutableStateFlow<String?>(null)

    private var lastBookIdToRestore: String? = null
    private var hasLoadedSettings = false

    private val liveBooks = combine(observeBooks(), failedBookIds, _selectedBook) { books, failedIds, selected ->
        books.map { book -> book.withLiveState(selected, failedIds) }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    init {
        liveBooks.onEach { books ->
            booksList = books
            val selected = _selectedBook.value
            if (selected != null && !selected.isExternal && books.none { it.id == selected.id }) {
                _selectedBook.value = null
            }
            restoreLastBook()
        }.launchIn(viewModelScope)

        combine(
            liveBooks,
            settings,
            selectedTab,
            combine(roots, scanStatus, legacyFolderToReselect) { roots, scan, legacyFolder -> Triple(roots, scan, legacyFolder) },
        ) { books, settings, tab, (roots, scan, legacyFolder) ->
            val visibleTabs = settings.visibleLibraryTabs
            val effectiveTab = listOfNotNull(tab, settings.libraryTab).firstOrNull { it in visibleTabs }
                ?: sortAndFilterBooks.defaultTab(books, visibleTabs)
            _libraryState.value = LibraryUiState(
                tab = effectiveTab,
                tabs = visibleTabs,
                sortOrder = settings.librarySortOrder,
                sortAscending = settings.librarySortAscending,
                layout = settings.libraryLayout,
                scanStatus = scan,
                inaccessibleRoots = roots.filterNot { it.isAccessible },
            )
            when {
                legacyFolder != null -> BooksListUiState.LegacyFolderMigration(legacyFolder)
                books.isNotEmpty() -> BooksListUiState.BookListSuccess(
                    sortAndFilterBooks(books, effectiveTab, settings.librarySortOrder, settings.librarySortAscending),
                )

                scan.isScanning -> BooksListUiState.Loading
                else -> BooksListUiState.NoBookSelected
            }
        }.onEach { _uiState.value = it }.launchIn(viewModelScope)

        settings.onEach { appSettings ->
            if (hasLoadedSettings && _selectedBook.value == null && appSettings.lastBookId != null) {
                lastBookIdToRestore = appSettings.lastBookId
                restoreLastBook()
            }
        }.launchIn(viewModelScope)

        playerController.state.onEach { playerState ->
            if (playerState.status == PlaybackStatus.READY || playerState.status == PlaybackStatus.ENDED) {
                _isAwaitingPlayback.value = false
            }
        }.launchIn(viewModelScope)

        playerController.state.map { it.bookId }.distinctUntilChanged().onEach { playingId ->
            val selected = _selectedBook.value
            if (playingId == null || playingId == selected?.id) return@onEach
            val book = booksList.firstOrNull { it.id == playingId } ?: return@onEach
            _showBookEndedDialog.value = false
            selectBook(book, book.getBookPosition())
        }.launchIn(viewModelScope)

        playerController.state.map { it.upNext != null }.distinctUntilChanged().onEach { hasUpNext ->
            if (hasUpNext) _showBookEndedDialog.value = false
        }.launchIn(viewModelScope)

        playerController.state.map { it.status == PlaybackStatus.ENDED }.distinctUntilChanged().onEach { hasEnded ->
            if (!hasEnded) return@onEach
            delay(QUEUE_DECISION_MS)
            val state = playerController.state.value
            if (state.status == PlaybackStatus.ENDED && state.upNext == null) closeFinishedBook()
        }.launchIn(viewModelScope)

        playerController.errors.onEach { error ->
            _isAwaitingPlayback.value = false
            when (error) {
                is PlayerError.TrackSkipped -> _skippedTrackMessages.tryEmit(error.fileName)
                PlayerError.NoiseReductionTooSlow -> _noiseReductionTurnedOff.tryEmit(Unit)
                PlayerError.BookUnplayable, PlayerError.StorageUnavailable -> {
                    selectedBook.value?.let { book ->
                        playerController.stop()
                        markBookAsError(book)
                    }
                    updateIsScreenLocked(false)
                    _collapsePlayerEvents.tryEmit(Unit)
                    if (error == PlayerError.StorageUnavailable) viewModelScope.launch { refreshLibrary() }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            val initialSettings = getAppSettings()
            lastBookIdToRestore = initialSettings.lastBookId
            hasLoadedSettings = true
            restoreLastBook()
            _shouldOpenPlayerInStartup.value = initialSettings.openPlayerOnStartup
            legacyFolderToReselect.value = prepareLegacyFolderMigration()
            refreshLibraryOnStart()
            importLegacyProgress()
        }
    }

    fun startingPoint(book: Book): Book {
        if (book.state != BookState.FINISHED) return book
        viewModelScope.launch { setBookState(book.id, BookState.NEW) }
        return book.fromStart()
    }

    private fun closeFinishedBook() {
        _showBookEndedDialog.value = true
        updateIsScreenLocked(false)
        playerController.stop()
        selectBook(null, null)
        _collapsePlayerEvents.tryEmit(Unit)
    }

    fun openBook(book: Book) {
        playerController.openBook(book, playWhenReady = true)
    }

    fun onPlayButtonClicked(book: Book, progress: Long) {
        val playerState = playerController.state.value
        when {
            playerState.isPlaying -> playerController.pause()
            playerState.isReadyWith(book) -> playerController.play()
            else -> reloadAndPlay(book, progress)
        }
    }

    fun onCoverClicked(book: Book, progress: Long) = onPlayButtonClicked(book, progress)

    private fun PlayerState.isReadyWith(book: Book): Boolean = status == PlaybackStatus.READY && bookId == book.id

    private fun reloadAndPlay(book: Book, progress: Long) {
        val bookAtProgress = when (book) {
            is BookFile -> book.copy(progress = progress)
            else -> book
        }

        _isAwaitingPlayback.value = true
        playerController.openBook(bookAtProgress, playWhenReady = true)
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun seekToTrack(trackIndex: Int) = playerController.seekTo(trackIndex, 0L)

    fun seekToBookPosition(bookPositionMs: Long) = afterSeek { playerController.seekToBookPosition(bookPositionMs) }

    fun seekBy(deltaMs: Long) = afterSeek { playerController.seekBy(deltaMs) }

    fun nextChapter() = afterSeek { playerController.nextChapter() }

    fun previousChapter() = afterSeek { playerController.previousChapter() }

    private fun afterSeek(seek: () -> Unit) {
        seek()
        syncPositionFromPlayer()
        persistProgress()
    }

    fun syncPositionFromPlayer() {
        val book = selectedBook.value ?: return
        val trackIndex = if (book is MultipleBooks) playerController.currentTrackIndex() else null
        setCurrentProgress(id = book.id, progress = playerController.currentPositionMs(), currentPosition = trackIndex)
    }

    fun setDefaultSpeed(speed: Float) = viewModelScope.launch { updateAppSettings { it.copy(defaultSpeed = speed) } }

    fun changeSpeed(speed: Float) = playerController.setSpeed(speed)

    fun stopPlayer() = playerController.stop()

    fun refreshCurrentProgress() {
        if (playerController.state.value.isPlaying) syncPositionFromPlayer()
    }

    fun setCurrentProgress(id: String, progress: Long, currentPosition: Int?) {
        _currentProgress.tryEmit(progress)
        val book = bookWithId(id) ?: return
        val updatedBook = book.withProgress(progress)

        selectBook(updatedBook, currentPosition)
    }

    fun persistProgress() {
        val book = _selectedBook.value ?: return
        viewModelScope.launch {
            saveListeningState(book.id, book.getBookPosition() ?: 0, book.progress, book.speed)
        }
    }

    fun selectBook(book: Book?, position: Int?) {
        _selectedBook.value = when (book) {
            is MultipleBooks -> position?.let { book.copy(currentBookPosition = it) } ?: book
            else -> book
        }
        if (book?.isExternal != true && book?.id != settings.value.lastBookId) {
            viewModelScope.launch { updateAppSettings { it.copy(lastBookId = book?.id) } }
        }
    }

    fun updateBookPosition(id: String, position: Int) {
        val book = booksList.firstOrNull { it.id == id } as? MultipleBooks ?: return
        selectBook(book.copy(currentBookPosition = position), position)
    }

    fun updateBookSpeed(id: String, speed: Float, currentPosition: Int?) {
        val book = bookWithId(id) ?: return
        val updatedBook = when (book) {
            is BookFile -> book.copy(speed = speed)
            is MultipleBooks -> book.copy(speed = speed)
            else -> return
        }
        selectBook(updatedBook, currentPosition)
        persistProgress()
    }

    fun removeBook(book: Book) {
        if (selectedBook.value?.id == book.id) selectBook(null, null)
        viewModelScope.launch { hideBook(book.id) }
    }

    fun markBookAsError(book: Book) {
        failedBookIds.value = failedBookIds.value + book.id
    }

    fun markBook(book: Book, state: BookState) {
        if (state == BookState.NEW && book.id == selectedBook.value?.id) {
            if (playerController.state.value.trackCount > 0) playerController.seekToBookPosition(0L)
            setCurrentProgress(id = book.id, progress = 0L, currentPosition = if (book is MultipleBooks) 0 else null)
        }
        viewModelScope.launch { setBookState(book.id, state) }
    }

    fun addBookmarkAtCurrentPosition() {
        val book = selectedBook.value ?: return
        val position = if (playerController.state.value.trackCount > 0) playerController.bookPositionMs() else book.bookPositionMs
        viewModelScope.launch { addBookmark(book.id, position)?.let(_bookmarkAdded::tryEmit) }
    }

    fun saveBookmarkNote(bookmarkId: Long, note: String) = viewModelScope.launch { updateBookmarkNote(bookmarkId, note) }

    fun requestPlay(bookId: String) {
        bookWithId(bookId)?.let { _playRequests.trySend(it) }
    }

    fun queue(book: Book, playNext: Boolean) = viewModelScope.launch {
        addToQueue(book.id, playNext)
        _queueAdded.tryEmit(playNext)
    }

    fun cancelUpNext() = playerController.cancelUpNext()

    fun converterEntry(): String? = getOpusEncoder().takeIf { IS_CONVERTER_ENABLED }

    fun startSleepTimer(mode: SleepTimerMode) = playerController.startSleepTimer(mode)

    fun cancelSleepTimer() = playerController.cancelSleepTimer()

    fun setShakeSensitivity(sensitivity: ShakeSensitivity) = viewModelScope.launch {
        updateAppSettings { it.copy(shakeSensitivity = sensitivity) }
    }

    fun addFolder(treeUri: String) = viewModelScope.launch { addLibraryRoot(treeUri) }

    fun addFile(documentUri: String) = viewModelScope.launch { addSingleFileBook(documentUri) }

    fun reconnectFolder(treeUri: String) = viewModelScope.launch {
        addLibraryRoot(treeUri)
        refreshLibrary()
    }

    fun completeLegacyMigration(treeUri: String?) = viewModelScope.launch {
        treeUri?.let { addLibraryRoot(it) }
        completeLegacyFolderMigration()
        legacyFolderToReselect.value = null
    }

    fun onRefreshClicked() {
        if (settings.value.reloadGuideShown) requestFullScan() else _showReloadGuide.value = true
    }

    fun dismissReloadGuide() {
        _showReloadGuide.value = false
        viewModelScope.launch { updateAppSettings { it.copy(reloadGuideShown = true) } }
    }

    fun dismissBookEndedDialog() {
        _showBookEndedDialog.value = false
    }

    fun updateIsScreenLocked(isDisabled: Boolean) {
        isScreenLocked.value = isDisabled
    }

    fun selectTab(tab: LibraryTab) {
        selectedTab.value = tab
    }

    fun selectSortOrder(sortOrder: LibrarySortOrder) = viewModelScope.launch {
        updateAppSettings { current ->
            if (current.librarySortOrder == sortOrder) {
                current.copy(librarySortAscending = !current.librarySortAscending)
            } else {
                current.copy(librarySortOrder = sortOrder, librarySortAscending = sortOrder.ascendingByDefault())
            }
        }
    }

    fun selectLayout(layout: LibraryLayout) = viewModelScope.launch {
        updateAppSettings { it.copy(libraryLayout = layout) }
    }

    private fun bookWithId(id: String): Book? = booksList.firstOrNull { it.id == id } ?: _selectedBook.value?.takeIf { it.id == id }

    fun resumeLastBook(startSleepTimer: Boolean = false) = viewModelScope.launch {
        playerController.state.first { it.isConnected }
        val book = _selectedBook.value?.takeUnless { it.isExternal } ?: getLastBook() ?: return@launch
        if (_selectedBook.value?.id != book.id) selectBook(book, book.getBookPosition())
        _expandPlayerEvents.trySend(Unit)
        val state = playerController.state.value
        when {
            state.isPlaying -> Unit
            state.status == PlaybackStatus.READY && state.bookId == book.id -> playerController.play()
            else -> reloadAndPlay(book, book.progress)
        }
        if (startSleepTimer) playerController.startSleepTimer(SleepTimerMode.Duration(settings.value.sleepDefaultMinutes))
    }

    fun openPlayer() = viewModelScope.launch {
        playerController.state.first { it.isConnected }
        val book = _selectedBook.value ?: getLastBook() ?: return@launch
        if (_selectedBook.value?.id != book.id) selectBook(book, book.getBookPosition())
        _expandPlayerEvents.trySend(Unit)
    }

    fun playExternalFile(documentUri: String) = viewModelScope.launch {
        playerController.state.first { it.isConnected }
        val book = openExternalFile(documentUri)
        if (book == null) {
            _externalFileEvents.trySend(false)
            return@launch
        }
        _selectedBook.value = book
        _expandPlayerEvents.trySend(Unit)
        reloadAndPlay(book, 0L)
        _externalFileEvents.trySend(true)
    }

    private fun restoreLastBook() {
        if (!hasLoadedSettings || booksList.isEmpty() || _selectedBook.value != null) return
        val lastBookId = lastBookIdToRestore ?: return
        lastBookIdToRestore = null
        booksList.firstOrNull { it.id == lastBookId && !it.hasError && it.state != BookState.FINISHED }
            ?.let { book -> _selectedBook.value = book }
    }

    private fun Book.withLiveState(selected: Book?, failedIds: Set<String>): Book {
        val base = if (selected != null && selected.id == id) selected.copyStructureFrom(this) else this
        return if (base.id in failedIds) base.withError() else base
    }

    private fun Book.copyStructureFrom(stored: Book): Book = when {
        this is MultipleBooks && stored is MultipleBooks -> stored.copy(
            progress = progress,
            speed = speed,
            currentBookPosition = currentBookPosition.coerceIn(0, stored.bookFileList.lastIndex),
        )

        this is BookFile && stored is BookFile -> stored.copy(progress = progress, speed = speed)
        else -> stored
    }

    private fun Book.withProgress(progress: Long): Book = when (this) {
        is MultipleBooks -> copy(progress = progress)
        is BookFile -> copy(progress = progress)
        else -> this
    }

    private fun Book.fromStart(): Book = when (this) {
        is MultipleBooks -> copy(progress = 0L, currentBookPosition = 0, state = BookState.NEW)
        is BookFile -> copy(progress = 0L, state = BookState.NEW)
        else -> this
    }

    private fun Book.withError(): Book = when (this) {
        is MultipleBooks -> copy(hasError = true)
        is BookFile -> copy(hasError = true)
        else -> this
    }

    private fun LibrarySortOrder.ascendingByDefault(): Boolean =
        this == LibrarySortOrder.TITLE || this == LibrarySortOrder.AUTHOR

    sealed class BooksListUiState {
        data class BookListSuccess(val audiobookList: List<Book>) : BooksListUiState()

        data object NoBookSelected : BooksListUiState()

        data object Loading : BooksListUiState()

        data class LegacyFolderMigration(val legacyFolderUri: String) : BooksListUiState()
    }

    data class LibraryUiState(
        val tab: LibraryTab = LibraryTab.ALL,
        val tabs: List<LibraryTab> = LibraryTab.entries,
        val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_PLAYED,
        val sortAscending: Boolean = false,
        val layout: LibraryLayout = LibraryLayout.LIST,
        val scanStatus: ScanStatus = ScanStatus(),
        val inaccessibleRoots: List<LibraryRoot> = emptyList(),
    )
}

private const val QUEUE_DECISION_MS = 1_500L
private const val IS_CONVERTER_ENABLED = false
