package com.mobyle.abbay.presentation.booklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.effects.AudioEffects
import com.effects.EqualizerPreset
import com.mobyle.abbay.infra.cast.CastButton
import com.mobyle.abbay.infra.navigation.LibraryDestinations
import com.mobyle.abbay.presentation.booklist.widgets.MiniPlayer
import com.mobyle.abbay.presentation.booklist.widgets.player.ChaptersToggle
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerActions
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerDisplay
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerSkips
import com.mobyle.abbay.presentation.booklist.widgets.player.SleepSheetSettings
import com.mobyle.abbay.presentation.documents.CompanionDocumentsViewModel
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.effects.AudioEffectsViewModel
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.options.PlaybackOptionsViewModel
import com.model.Book
import com.model.BookState
import kotlinx.coroutines.launch

/** What the player drives and where its menu leads, shared by the library's sheet and the player pane beside any screen. */
class LibraryPlayerDependencies(
    val viewModel: BooksListViewModel,
    val effectsViewModel: AudioEffectsViewModel,
    val optionsViewModel: PlaybackOptionsViewModel,
    val documentsViewModel: CompanionDocumentsViewModel,
    val navigation: PlayerNavigation,
)

class PlayerNavigation(
    val settings: () -> Unit = {},
    val bookDetails: (bookId: String) -> Unit = {},
    val bookmarks: (bookId: String) -> Unit = {},
    val history: (bookId: String) -> Unit = {},
    val destinations: LibraryDestinations = LibraryDestinations(),
)

/**
 * The player for [book]: in the library's bottom sheet ([isPane] false, collapsing to the mini player), or as a
 * pane beside browsing ([isPane] true, closing with ✕). Its chapters follow [presentation], so a reflow keeps them.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryPlayer(
    book: Book,
    sheetState: BottomSheetScaffoldState,
    presentation: PlayerPresentation,
    dependencies: LibraryPlayerDependencies,
    isPane: Boolean,
    modifier: Modifier = Modifier,
) {
    val viewModel = dependencies.viewModel
    val scope = rememberCoroutineScope()
    val playerState by viewModel.playerState.collectAsState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()
    val isAwaitingPlayback by viewModel.isAwaitingPlayback.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val playbackOptions by dependencies.optionsViewModel.options.collectAsState()
    val audioEffects by dependencies.effectsViewModel.effects.collectAsState()
    val equalizerPresets by dependencies.effectsViewModel.presets.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        MiniPlayer(
            book = book,
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
            actions = playerActions(book, dependencies, showsCastButton = appSettings.showCastButton),
            effects = dependencies.effectsPanel(isCasting = playerState.isCasting, effects = audioEffects, presets = equalizerPresets),
            options = PlaybackOptionsPanel(
                options = playbackOptions,
                onChange = dependencies.optionsViewModel::change,
                onSetLoopPoint = dependencies.optionsViewModel::setLoopPoint,
            ),
            sleepSettings = SleepSheetSettings(
                defaultMinutes = appSettings.sleepDefaultMinutes,
                isShakeEnabled = appSettings.shakeToExtend,
                shakeSensitivity = appSettings.shakeSensitivity,
            ),
            display = PlayerDisplay(appSettings.progressLabels, appSettings.hiddenQuickActions),
            chapters = presentation.chaptersToggle(),
            onClose = if (isPane) ({ scope.launch { presentation.close() } }) else null,
            onExpand = { scope.launch { presentation.open() } },
        )
    }
}

private fun PlayerPresentation.chaptersToggle() = ChaptersToggle(
    isOpen = isShowingChapters,
    onToggle = { setChapters(open = !isShowingChapters) },
    onClose = { setChapters(open = false) },
)

private fun playerActions(book: Book, dependencies: LibraryPlayerDependencies, showsCastButton: Boolean): PlayerActions {
    val viewModel = dependencies.viewModel
    val navigation = dependencies.navigation
    return PlayerActions(
        onPlayButtonClick = { progress -> viewModel.onPlayButtonClicked(book, progress) },
        onCoverClick = { progress -> viewModel.onCoverClicked(book, progress) },
        onSeekToBookPosition = viewModel::seekToBookPosition,
        onSeekBy = viewModel::seekBy,
        onNextChapter = viewModel::nextChapter,
        onPreviousChapter = viewModel::previousChapter,
        onSpeedChange = { speed ->
            viewModel.changeSpeed(speed)
            viewModel.updateBookSpeed(id = book.id, speed = speed, currentPosition = book.getBookPosition())
        },
        onApplyBookSpeed = viewModel::changeSpeed,
        onSetDefaultSpeed = { speed -> viewModel.setDefaultSpeed(speed) },
        onTrackChanged = viewModel::syncPositionFromPlayer,
        onLockScreen = viewModel::updateIsScreenLocked,
        onOpenDetails = { navigation.bookDetails(book.id) },
        onMarkFinished = { viewModel.markBook(book, BookState.FINISHED) },
        onOpenSettings = navigation.settings,
        onAddBookmark = viewModel::addBookmarkAtCurrentPosition,
        onStartSleepTimer = viewModel::startSleepTimer,
        onCancelSleepTimer = viewModel::cancelSleepTimer,
        onShakeSensitivityChange = viewModel::setShakeSensitivity,
        onOpenBookmarks = { navigation.bookmarks(book.id) },
        onOpenHistory = { navigation.history(book.id) },
        onOpenCharacters = { navigation.destinations.characters(book.id) },
        onOpenStatistics = { navigation.destinations.bookStatistics(book.id) },
        onOpenBookText = { dependencies.documentsViewModel.show(book.id) },
        onOpenText = { navigation.destinations.text(book.id) },
        routeButton = { if (showsCastButton) CastButton() },
    )
}

private fun LibraryPlayerDependencies.effectsPanel(
    isCasting: Boolean,
    effects: AudioEffects?,
    presets: List<EqualizerPreset>,
) = AudioEffectsPanel(
    isCasting = isCasting,
    effects = effects,
    presets = presets,
    onChange = effectsViewModel::change,
    onApplyEqualizerToAll = { gains -> effectsViewModel.applyEqualizerToAll(gains) },
    onSavePreset = { name, gains -> effectsViewModel.savePreset(name, gains) },
    onDeletePreset = { presetId -> effectsViewModel.deletePreset(presetId) },
)
