package com.mobyle.abbay.presentation.booklist

import android.app.Activity
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.library.FolderLocation
import com.library.FolderTree
import com.library.LibraryLayout
import com.mobyle.abbay.R
import com.mobyle.abbay.infra.ads.AdBanner
import com.mobyle.abbay.infra.cast.CastButton
import com.mobyle.abbay.infra.navigation.LibraryDestinations
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.BookListSuccess
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.LegacyFolderMigration
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.Loading
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState.NoBookSelected
import com.mobyle.abbay.presentation.booklist.widgets.BookActionsMenu
import com.mobyle.abbay.presentation.booklist.widgets.BookCompactItem
import com.mobyle.abbay.presentation.booklist.widgets.BookGridItem
import com.mobyle.abbay.presentation.booklist.widgets.BookItem
import com.mobyle.abbay.presentation.booklist.widgets.BookListTopBar
import com.mobyle.abbay.presentation.booklist.widgets.BookMenuExtras
import com.mobyle.abbay.presentation.booklist.widgets.LegacyFolderMigrationContent
import com.mobyle.abbay.presentation.booklist.widgets.LibraryControls
import com.mobyle.abbay.presentation.booklist.widgets.LibraryMenu
import com.mobyle.abbay.presentation.booklist.widgets.LibraryTabEmptyState
import com.mobyle.abbay.presentation.booklist.widgets.MiniPlayer
import com.mobyle.abbay.presentation.booklist.widgets.ReconnectBanner
import com.mobyle.abbay.presentation.booklist.widgets.UpNextBanner
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerActions
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerDisplay
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerSkips
import com.mobyle.abbay.presentation.booklist.widgets.player.SleepSheetSettings
import com.mobyle.abbay.presentation.bookmarks.BookmarkNoteDialog
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.theme.AbbayTextStyles
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.documents.CompanionDocumentsDialog
import com.mobyle.abbay.presentation.documents.CompanionDocumentsViewModel
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.effects.AudioEffectsViewModel
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.options.PlaybackOptionsViewModel
import com.mobyle.abbay.presentation.utils.LaunchedEffectAndCollect
import com.mobyle.abbay.presentation.utils.intermediateProgress
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.model.BookFile
import com.model.BookState
import com.model.Bookmark
import com.model.MultipleBooks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mobyle.abbay.presentation.booklist.widgets.player.progressFraction as playerProgressFraction

private const val UPDATE_PROGRESS_REFRESH_RATE = 3

@Composable
fun BooksListScreen(
    viewModel: BooksListViewModel,
    navigateToSettings: () -> Unit,
    navigateToBookDetails: (bookId: String) -> Unit,
    navigateToBookmarks: (bookId: String) -> Unit = {},
    navigateToHistory: (bookId: String) -> Unit = {},
    navigateTo: LibraryDestinations = LibraryDestinations(),
    effectsViewModel: AudioEffectsViewModel = hiltViewModel(),
    optionsViewModel: PlaybackOptionsViewModel = hiltViewModel(),
    documentsViewModel: CompanionDocumentsViewModel = hiltViewModel(),
) {
    val asyncScope = rememberCoroutineScope()
    val fileFilterList = arrayOf("audio/*")

    // States
    val bottomSheetState = rememberBottomSheetScaffoldState()
    val booksListState by viewModel.uiState.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()
    val currentProgress by viewModel.currentProgress.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val hasBookSelected by viewModel.hasBookSelected.collectAsState(false)
    val activity = LocalContext.current as Activity
    val showErrorDialog = remember { mutableStateOf(false) }
    val hasSelectedFolder by viewModel.hasSelectedFolder.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val hasBookEnded by viewModel.showBookEndedDialog.collectAsState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()
    val shouldOpenPlayerInStartup by viewModel.shouldOpenPlayerInStartup.collectAsState()
    val hasOpenedPlayerOnStartup = remember { mutableStateOf(false) }
    // Tablets and unfolded foldables show the library and the player side by side.
    val isWide = LocalConfiguration.current.screenWidthDp >= LIST_DETAIL_MIN_WIDTH_DP
    val playerPaneState = rememberBottomSheetScaffoldState(bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded))

    // Expand only once the sheet has its peek height, i.e. once a book is selected.
    val canOpenPlayerOnStartup = hasBookSelected && selectedBook?.hasError == false
    LaunchedEffect(shouldOpenPlayerInStartup, canOpenPlayerOnStartup) {
        if (shouldOpenPlayerInStartup && canOpenPlayerOnStartup && !hasOpenedPlayerOnStartup.value) {
            // The sheet recomputes its anchors after the peek height changes; expanding earlier leaves it hidden.
            repeat(2) { withFrameNanos { } }
            bottomSheetState.bottomSheetState.expand()
            hasOpenedPlayerOnStartup.value = true
        }
    }

    LaunchedEffect(hasBookEnded) {
        if (hasBookEnded) {
            bottomSheetState.bottomSheetState.collapse()
        }
    }

    // Launchers
    val openFileSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { filesList ->
        filesList.forEach { uri -> viewModel.addFile(uri.toString()) }
    }

    val openFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.addFolder(it.toString()) }
    }

    val reconnectFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.reconnectFolder(it.toString()) }
    }

    val legacyFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.completeLegacyMigration(it.toString()) }
    }

    // SideEffects
    BackHandler {
        if (!isScreenLocked) {
            asyncScope.launch {
                if (bottomSheetState.bottomSheetState.isCollapsed) {
                    activity.finishAffinity()
                } else {
                    bottomSheetState.bottomSheetState.collapse()
                }
            }
        }
    }

    LaunchedEffectAndCollect(viewModel.isPlaying) {
        var count = 0

        while (it == true) {
            viewModel.refreshCurrentProgress()

            if (count == UPDATE_PROGRESS_REFRESH_RATE) {
                viewModel.persistProgress()
                count = 0
            }

            delay(1000)
            count++
        }
    }

    LaunchedEffect(Unit) {
        viewModel.collapsePlayerEvents.collect {
            bottomSheetState.bottomSheetState.collapse()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.expandPlayerEvents.collect { bottomSheetState.bottomSheetState.expand() }
    }

    val externalFileMessage = stringResource(R.string.external_file_playing)
    val externalFileUnreadable = stringResource(R.string.external_file_unreadable)
    val addFolderLabel = stringResource(R.string.external_file_add_folder)
    LaunchedEffect(Unit) {
        viewModel.externalFileEvents.collect { isPlaying ->
            if (!isPlaying) {
                bottomSheetState.snackbarHostState.showSnackbar(externalFileUnreadable)
            } else if (bottomSheetState.snackbarHostState.showSnackbar(externalFileMessage, addFolderLabel) ==
                SnackbarResult.ActionPerformed
            ) {
                openFolderSelector.launch(null)
            }
        }
    }

    val noiseReductionOffMessage = stringResource(R.string.effects_noise_reduction_disabled)
    LaunchedEffect(Unit) {
        viewModel.noiseReductionTurnedOff.collect { bottomSheetState.snackbarHostState.showSnackbar(noiseReductionOffMessage) }
    }

    val skippedTrackMessage = stringResource(R.string.playback_track_skipped)
    LaunchedEffect(Unit) {
        viewModel.skippedTrackMessages.collect { fileName ->
            bottomSheetState.snackbarHostState.showSnackbar(skippedTrackMessage.format(fileName))
        }
    }

    LaunchedEffect(selectedBook?.id) {
        effectsViewModel.load(selectedBook?.id)
        optionsViewModel.load(selectedBook?.id?.takeUnless { selectedBook?.isExternal == true })
    }
    val playbackOptions by optionsViewModel.options.collectAsState()
    val audioEffects by effectsViewModel.effects.collectAsState()
    val equalizerPresets by effectsViewModel.presets.collectAsState()

    var bookmarkToAnnotate by remember { mutableStateOf<Bookmark?>(null) }
    val bookmarkAddedMessage = stringResource(R.string.player_bookmark_added)
    val addNoteLabel = stringResource(R.string.player_bookmark_add_note)
    LaunchedEffect(Unit) {
        viewModel.bookmarkAdded.collect { bookmark ->
            val result = bottomSheetState.snackbarHostState.showSnackbar(bookmarkAddedMessage, addNoteLabel)
            if (result == SnackbarResult.ActionPerformed) bookmarkToAnnotate = bookmark
        }
    }
    bookmarkToAnnotate?.let { bookmark ->
        BookmarkNoteDialog(
            initialNote = bookmark.note,
            onSave = { note ->
                viewModel.saveBookmarkNote(bookmark.id, note)
                bookmarkToAnnotate = null
            },
            onDismiss = { bookmarkToAnnotate = null },
        )
    }

    // "Mark as new" resets the position, so it asks first.
    var bookToMarkNew by remember { mutableStateOf<Book?>(null) }
    val markBook: (Book, BookState) -> Unit = { book, state ->
        if (state == BookState.NEW) bookToMarkNew = book else viewModel.markBook(book, state)
    }
    bookToMarkNew?.let { book ->
        AbbayAlertDialog(
            onDismissRequest = { bookToMarkNew = null },
            title = { Text(stringResource(R.string.mark_new_title)) },
            text = { Text(stringResource(R.string.mark_new_body)) },
            confirmButton = {
                AbbayTextButton(onClick = {
                    viewModel.markBook(book, BookState.NEW)
                    bookToMarkNew = null
                }) { Text(stringResource(R.string.book_action_mark_new)) }
            },
            dismissButton = { AbbayTextButton(onClick = { bookToMarkNew = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    LaunchedEffect(selectedBook?.id, selectedBook?.hasError) {
        if (selectedBook?.hasError == true) {
            viewModel.stopPlayer()
            showErrorDialog.value = true
            bottomSheetState.bottomSheetState.collapse()
            viewModel.updateIsScreenLocked(false)
        }
    }

    val onBookClick: (Book) -> Unit = { tapped ->
        // A finished book starts over.
        val book = if (tapped.hasError) tapped else viewModel.startingPoint(tapped)
        val startsOver = book !== tapped
        if ((selectedBook?.id != book.id || startsOver) && !book.hasError) {
            viewModel.setCurrentProgress(
                id = book.id,
                progress = book.progress,
                currentPosition = book.getBookPosition()
            )
            viewModel.selectBook(book, book.getBookPosition())
            viewModel.openBook(book)
        }

        if (book.hasError) {
            showErrorDialog.value = true
            if (selectedBook?.id == book.id) {
                viewModel.selectBook(null, null)
            }
        } else {
            asyncScope.launch {
                bottomSheetState.bottomSheetState.expand()
                bottomSheetState.bottomSheetState.expand()
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.playRequests.collect { book -> onBookClick(book) } }

    val queueAddedMessage = stringResource(R.string.queue_added)
    val playsNextMessage = stringResource(R.string.queue_plays_next)
    LaunchedEffect(Unit) {
        viewModel.queueAdded.collect { playNext ->
            bottomSheetState.snackbarHostState.showSnackbar(if (playNext) playsNextMessage else queueAddedMessage)
        }
    }

    CompanionDocumentsDialog(documentsViewModel)

    val playerPane: @Composable (BottomSheetScaffoldState) -> Unit = { sheetState ->
        selectedBook?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                val isAwaitingPlayback by viewModel.isAwaitingPlayback.collectAsState()

                val appSettings by viewModel.appSettings.collectAsState()

                MiniPlayer(
                    book = it,
                    playerState = playerState,
                    isAwaitingPlayback = isAwaitingPlayback,
                    isScreenLocked = isScreenLocked,
                    scaffoldState = sheetState,
                    skips = PlayerSkips(
                        smallBackSeconds = appSettings.smallBackSeconds,
                        smallForwardSeconds = appSettings.smallForwardSeconds,
                        largeBackSeconds = appSettings.largeBackSeconds,
                        largeForwardSeconds = appSettings.largeForwardSeconds,
                    ),
                    actions = PlayerActions(
                        onPlayButtonClick = { progress -> viewModel.onPlayButtonClicked(it, progress) },
                        onCoverClick = { progress -> viewModel.onCoverClicked(it, progress) },
                        onSeekToBookPosition = viewModel::seekToBookPosition,
                        onSeekBy = viewModel::seekBy,
                        onNextChapter = viewModel::nextChapter,
                        onPreviousChapter = viewModel::previousChapter,
                        onSpeedChange = { speed ->
                            viewModel.changeSpeed(speed)
                            viewModel.updateBookSpeed(id = it.id, speed = speed, currentPosition = it.getBookPosition())
                        },
                        onApplyBookSpeed = viewModel::changeSpeed,
                        onSetDefaultSpeed = { speed -> viewModel.setDefaultSpeed(speed) },
                        onTrackChanged = viewModel::syncPositionFromPlayer,
                        onLockScreen = viewModel::updateIsScreenLocked,
                        onOpenDetails = { navigateToBookDetails(it.id) },
                        onMarkFinished = { viewModel.markBook(it, BookState.FINISHED) },
                        onOpenSettings = navigateToSettings,
                        onAddBookmark = viewModel::addBookmarkAtCurrentPosition,
                        onStartSleepTimer = viewModel::startSleepTimer,
                        onCancelSleepTimer = viewModel::cancelSleepTimer,
                        onShakeSensitivityChange = viewModel::setShakeSensitivity,
                        onOpenBookmarks = { navigateToBookmarks(it.id) },
                        onOpenHistory = { navigateToHistory(it.id) },
                        onOpenCharacters = { navigateTo.characters(it.id) },
                        onOpenStatistics = { navigateTo.bookStatistics(it.id) },
                        onOpenBookText = { documentsViewModel.show(it.id) },
                        onOpenText = { navigateTo.text(it.id) },
                        routeButton = { if (appSettings.showCastButton) CastButton() },
                    ),
                    effects = AudioEffectsPanel(
                        isCasting = playerState.isCasting,
                        effects = audioEffects,
                        presets = equalizerPresets,
                        onChange = effectsViewModel::change,
                        onApplyEqualizerToAll = { gains -> effectsViewModel.applyEqualizerToAll(gains) },
                        onSavePreset = { name, gains -> effectsViewModel.savePreset(name, gains) },
                        onDeletePreset = { presetId -> effectsViewModel.deletePreset(presetId) },
                    ),
                    options = PlaybackOptionsPanel(
                        options = playbackOptions,
                        onChange = optionsViewModel::change,
                        onSetLoopPoint = optionsViewModel::setLoopPoint,
                    ),
                    sleepSettings = SleepSheetSettings(
                        defaultMinutes = appSettings.sleepDefaultMinutes,
                        isShakeEnabled = appSettings.shakeToExtend,
                        shakeSensitivity = appSettings.shakeSensitivity,
                    ),
                    display = PlayerDisplay(appSettings.progressLabels, appSettings.hiddenQuickActions),
                )
            }
        }
    }

    // The mini player covers the bottom of the library; the ad banner sits above it.
    val miniPlayerHeight = if (hasBookSelected && !isWide) 72.dp else 0.dp
    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetState,
            sheetContent = { if (!isWide) playerPane(bottomSheetState) },
            sheetGesturesEnabled = !isScreenLocked && !isWide,
            sheetPeekHeight = miniPlayerHeight,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    Scaffold(
                        topBar = {
                            BookListTopBar(
                                openFolderSelector = {
                                    openFolderSelector.launch(null)
                                },
                                openSettings = navigateToSettings,
                                openFileSelector = {
                                    openFileSelector.launch(fileFilterList)
                                },
                                onRefresh = viewModel::onRefreshClicked,
                                hasSelectedFolder = hasSelectedFolder,
                                isContentEnabled = booksListState is BookListSuccess || booksListState is NoBookSelected,
                                isRefreshing = isRefreshing,
                                more = LibraryMenu(
                                    onSearch = navigateTo.search,
                                    onQueue = navigateTo.queue,
                                    onFolders = { navigateTo.folders(FolderLocation.TOP) },
                                    onStatistics = navigateTo.statistics,
                                ),
                            )
                        },
                        backgroundColor = MaterialTheme.colorScheme.primary
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            libraryState.inaccessibleRoots.forEach { root ->
                                ReconnectBanner(folderName = root.displayName) {
                                    reconnectFolderSelector.launch(documentUriOfTree(root.treeUri))
                                }
                            }

                            // Shown in every state: Refresh on an empty library opens it too.
                            val showReloadGuide by viewModel.showReloadGuide.collectAsState()
                            if (showReloadGuide) {
                                AbbayActionDialog(
                                    onDismiss = viewModel::dismissReloadGuide,
                                    title = stringResource(R.string.reload_books_dialog_title),
                                    body = stringResource(R.string.reload_books_dialog_body),
                                    actionButtonTitle = stringResource(R.string.got_it),
                                    onAction = viewModel::dismissReloadGuide,
                                    icon = Icons.Default.Refresh,
                                    isDestructive = false,
                                    showCancel = false,
                                )
                            }

                            LaunchedEffect(booksListState) {
                                if (booksListState is NoBookSelected) {
                                    viewModel.stopPlayer()
                                    viewModel.selectBook(null, null)
                                }
                            }

                            when (val state = booksListState) {
                                is BookListSuccess -> {
                                    val bookToDelete = remember { mutableStateOf<Book?>(null) }
                                    val showDeleteDialog = remember { mutableStateOf(false) }
                                    val bookWithActions = remember { mutableStateOf<Book?>(null) }

                                    LaunchedEffect(selectedBook?.id) {
                                        selectedBook?.let { book ->
                                            viewModel.setCurrentProgress(
                                                id = book.id,
                                                progress = book.progress,
                                                currentPosition = book.getBookPosition()
                                            )
                                        }
                                    }

                                    LibraryControls(
                                        tab = libraryState.tab,
                                        tabs = libraryState.tabs,
                                        sortOrder = libraryState.sortOrder,
                                        sortAscending = libraryState.sortAscending,
                                        layout = libraryState.layout,
                                        scanStatus = libraryState.scanStatus,
                                        onTabSelected = viewModel::selectTab,
                                        onSortOrderSelected = { viewModel.selectSortOrder(it) },
                                        onLayoutSelected = { viewModel.selectLayout(it) },
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        val progressLabelOf: (Book) -> String = { book ->
                                            progressLabel(book, selectedBook, isPlaying, currentProgress)
                                        }

                                        if (state.audiobookList.isEmpty()) {
                                            LibraryTabEmptyState(
                                                tab = libraryState.tab,
                                                visibleTabs = libraryState.tabs,
                                                isScanning = libraryState.scanStatus.isScanning,
                                                onShowTab = viewModel::selectTab,
                                            )
                                        } else {
                                            when (libraryState.layout) {
                                                LibraryLayout.LIST -> LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.primary)
                                                ) {
                                                    items(items = state.audiobookList, key = { book -> book.id }) { book ->
                                                        SwipeToRemoveBook(
                                                            onSwipe = {
                                                                bookToDelete.value = book
                                                                showDeleteDialog.value = true
                                                            }
                                                        ) {
                                                            BookListRow(
                                                                book = book,
                                                                selectedBook = selectedBook,
                                                                currentTrackIndex = playerState.currentTrackIndex,
                                                                progressLabel = progressLabelOf(book),
                                                                onLongClick = { bookWithActions.value = book },
                                                                onClick = { onBookClick(book) }
                                                            )
                                                        }
                                                    }
                                                }

                                                LibraryLayout.GRID -> LazyVerticalGrid(
                                                    columns = GridCells.Adaptive(minSize = 140.dp),
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    items(items = state.audiobookList, key = { book -> book.id }) { book ->
                                                        BookGridItem(
                                                            book = book,
                                                            progressLabel = progressLabelOf(book),
                                                            progressFraction = progressFraction(book),
                                                            onLongClick = { bookWithActions.value = book },
                                                            onClick = { onBookClick(book) }
                                                        )
                                                    }
                                                }

                                                LibraryLayout.COMPACT -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                    items(items = state.audiobookList, key = { book -> book.id }) { book ->
                                                        BookCompactItem(
                                                            book = book,
                                                            progressLabel = progressLabelOf(book),
                                                            onLongClick = { bookWithActions.value = book },
                                                            onClick = { onBookClick(book) }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        BookActionsMenu(
                                            book = bookWithActions.value,
                                            onDismiss = { bookWithActions.value = null },
                                            onPlay = onBookClick,
                                            onMark = markBook,
                                            onRemove = { book ->
                                                bookToDelete.value = book
                                                showDeleteDialog.value = true
                                            },
                                            onEditDetails = { book -> navigateToBookDetails(book.id) },
                                            extras = BookMenuExtras(
                                                onQueue = { book, playNext -> viewModel.queue(book, playNext) },
                                                onOpenText = { book -> documentsViewModel.show(book.id) },
                                                onShowInFolder = { book -> navigateTo.folders(FolderTree.locationOf(book)) },
                                            ),
                                        )
                                    }

                                    AdBanner(modifier = Modifier.padding(bottom = miniPlayerHeight))

                                    if (showDeleteDialog.value) {
                                        AbbayActionDialog(
                                            onDismiss = {
                                                showDeleteDialog.value = false
                                                bookToDelete.value = null
                                            },
                                            title = stringResource(R.string.delete_book_dialog_title),
                                            body = stringResource(R.string.delete_book_dialog_body),
                                            actionButtonTitle = stringResource(R.string.delete),
                                            onAction = {
                                                bookToDelete.value?.let { book ->
                                                    if (selectedBook?.id == book.id) viewModel.stopPlayer()
                                                    viewModel.removeBook(book)
                                                }
                                                showDeleteDialog.value = false
                                                bookToDelete.value = null
                                            },
                                        )
                                    }

                                    if (showErrorDialog.value) {
                                        FileNotFoundDialog(
                                            showErrorDialog = showErrorDialog,
                                            onShown = {
                                                viewModel.stopPlayer()
                                                selectedBook?.let { if (!it.hasError) viewModel.markBookAsError(it) }
                                            },
                                            onDismiss = { viewModel.selectBook(null, null) }
                                        )
                                    }

                                    if (hasBookEnded) {
                                        AbbayActionDialog(
                                            onDismiss = viewModel::dismissBookEndedDialog,
                                            title = stringResource(R.string.book_ended_dialog_title),
                                            body = stringResource(R.string.book_ended_dialog_body),
                                            actionButtonTitle = stringResource(R.string.ok),
                                            onAction = viewModel::dismissBookEndedDialog,
                                            icon = Icons.Default.CheckCircle,
                                            isDestructive = false,
                                            showCancel = false,
                                        )
                                    }
                                }

                                is NoBookSelected -> EmptyLibrary(
                                    onAddFile = { openFileSelector.launch(fileFilterList) },
                                    onAddFolder = { openFolderSelector.launch(null) }
                                )

                                is Loading -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                is LegacyFolderMigration -> LegacyFolderMigrationContent(
                                    onChooseFolder = { legacyFolderSelector.launch(documentUriOfTree(state.legacyFolderUri)) },
                                    onSkip = { viewModel.completeLegacyMigration(null) }
                                )
                            }
                        }
                    }
                }
                if (isWide && selectedBook != null) {
                    Box(
                        modifier = Modifier
                            .width(PLAYER_PANE_WIDTH)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface),
                    ) { playerPane(playerPaneState) }
                }
            }
        }

        UpNextBanner(
            upNext = playerState.upNext,
            onCancel = viewModel::cancelUpNext,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private const val LIST_DETAIL_MIN_WIDTH_DP = 840
private val PLAYER_PANE_WIDTH = 420.dp

@Composable
private fun SwipeToRemoveBook(onSwipe: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) onSwipe()
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_content_description_short),
                    tint = AbbayColors.content
                )
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                content()
            }
        },
        enableDismissFromEndToStart = true,
    )
}

@Composable
private fun BookListRow(
    book: Book,
    selectedBook: Book?,
    currentTrackIndex: Int,
    progressLabel: String,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    when (book) {
        is MultipleBooks -> BookItem(
            book = book,
            currentMediaIndex = book.currentBookPosition + 1,
            isSelected = book.id == (selectedBook as? BookFile)?.id,
            intermediaryProgress = book.bookFileList.intermediateProgress(book.currentBookPosition),
            progress = progressLabel,
            onLongClick = onLongClick,
            onClick = onClick
        )

        is BookFile -> BookItem(
            book = book,
            currentMediaIndex = currentTrackIndex + 1,
            isSelected = book.id == (selectedBook as? BookFile)?.id,
            intermediaryProgress = 0L,
            progress = progressLabel,
            onLongClick = onLongClick,
            onClick = onClick
        )
    }
}

/** Elapsed time of [book]: live for the selected book while playing, saved otherwise. */
private fun progressLabel(book: Book, selectedBook: Book?, isPlaying: Boolean, currentProgress: Long): String {
    val intermediaryProgress = (book as? MultipleBooks)?.bookFileList?.intermediateProgress(book.currentBookPosition) ?: 0L
    val isLive = isPlaying && book.id == selectedBook?.id
    return intermediaryProgress.plus(if (isLive) currentProgress else book.progress).toHHMMSS()
}

/** Finished books are full; a book within 30 s of its end counts as 100 %. */
private fun progressFraction(book: Book): Float =
    if (book.state == BookState.FINISHED) 1f else playerProgressFraction(book.bookPositionMs, book.duration)

/** The picker opens at [treeUri] when the provider supports it (EXTRA_INITIAL_URI). */
private fun documentUriOfTree(treeUri: String): Uri? = runCatching {
    val tree = Uri.parse(treeUri)
    DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
}.getOrNull()

@Composable
private fun EmptyLibrary(onAddFile: () -> Unit, onAddFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onAddFile,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.secondary,
            )
        ) {
            Text(
                stringResource(R.string.no_book_selected_primary_button_title),
                style = AbbayTextStyles.buttonTextLarge
                    .copy(color = MaterialTheme.colorScheme.primary)
            )
        }

        Text(
            stringResource(R.string.no_book_selected_or),
            style = AbbayTextStyles.subtitleText
        )

        Button(
            onClick = onAddFolder,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.secondary,
            )
        ) {
            Text(
                text = stringResource(R.string.no_book_selected_secondary_button_title),
                style = AbbayTextStyles.buttonTextLarge
                    .copy(color = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun FileNotFoundDialog(showErrorDialog: MutableState<Boolean>, onShown: () -> Unit, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { onShown() }

    AbbayAlertDialog(
        onDismissRequest = {
            showErrorDialog.value = false
            onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    stringResource(R.string.file_not_found_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.file_not_found_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    stringResource(R.string.file_might_be_moved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.7f
                    ),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            AbbayTextButton(
                onClick = {
                    showErrorDialog.value = false
                    onDismiss()
                }
            ) {
                Text(
                    stringResource(R.string.ok),
                    color = AbbayColors.content
                )
            }
        }
    )
}
