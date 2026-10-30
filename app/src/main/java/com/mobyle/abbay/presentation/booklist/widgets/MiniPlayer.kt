package com.mobyle.abbay.presentation.booklist.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.models.LayoutId
import com.mobyle.abbay.presentation.booklist.widgets.player.ChaptersPanel
import com.mobyle.abbay.presentation.booklist.widgets.player.ChaptersToggle
import com.mobyle.abbay.presentation.booklist.widgets.player.MiniPlayerRow
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerActions
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerControls
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerCover
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerDisplay
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerMenuExtras
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerPosition
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerSkips
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerTopBar
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayingState
import com.mobyle.abbay.presentation.booklist.widgets.player.ScreenLockedAlert
import com.mobyle.abbay.presentation.booklist.widgets.player.SleepSheetSettings
import com.mobyle.abbay.presentation.booklist.widgets.player.timeLeftLabel
import com.mobyle.abbay.presentation.common.theme.coverTintBackground
import com.mobyle.abbay.presentation.common.theme.rememberCoverTint
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.utils.currentFraction
import com.model.Book
import com.player.PlaybackStatus
import com.player.PlayerState
import kotlinx.coroutines.launch

/**
 * The YouTube-style player of AbbayAndroid: a mini player docked under the book list that expands to the full
 * player through the existing MotionLayout scene. The player functions live in the
 * expanded state.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMotionApi::class, ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    book: Book,
    playerState: PlayerState,
    isAwaitingPlayback: Boolean,
    isScreenLocked: Boolean,
    scaffoldState: BottomSheetScaffoldState,
    skips: PlayerSkips,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    sleepSettings: SleepSheetSettings = SleepSheetSettings(),
    effects: AudioEffectsPanel = AudioEffectsPanel(),
    options: PlaybackOptionsPanel = PlaybackOptionsPanel(),
    display: PlayerDisplay = PlayerDisplay(),
) {
    val fraction = scaffoldState.currentFraction.coerceIn(0f, 1f)
    val isExpanded = fraction == 1f
    val position = PlayerPosition.of(book)
    val playerIcon = playingStateOf(playerState, isAwaitingPlayback)
    val tint = rememberCoverTint(book.thumbnail)
    val chapters = rememberChaptersToggle(isExpanded)
    PlayerEffects(book, playerState, actions)

    MotionLayout(
        motionScene = rememberPlayerMotionScene(),
        progress = fraction,
        // The cover's colour behind the whole player: flat on the mini player, shading to dark as it opens (AbbayiOS).
        modifier = modifier.fillMaxSize().coverTintBackground(tint, gradientStrength = fraction),
    ) {
        MiniPlayerRow(
            book = book,
            position = position,
            playerIcon = playerIcon,
            isScreenLocked = isScreenLocked,
            onPlayButtonClick = { actions.onPlayButtonClick(book.progress) },
            modifier = Modifier.fillMaxWidth().layoutId(LayoutId.MINI_PLAYER.id),
        )

        PlayerControls(
            book = book,
            position = position,
            playerIcon = playerIcon,
            skips = skips,
            actions = actions,
            modifier = Modifier.fillMaxWidth().layoutId(LayoutId.CONTENT.id).padding(controlsPadding),
            chapters = chapters,
            sleepTimer = playerState.sleepTimer,
            sleepSettings = sleepSettings,
            effects = effects,
            options = options,
            display = display,
        )

        CoverAndChapters(
            book = book,
            playerIcon = playerIcon,
            skips = skips,
            actions = actions,
            tint = tint,
            chapters = chapters,
            gesturesEnabled = isExpanded && !isScreenLocked,
            modifier = Modifier.padding(8.dp).layoutId(LayoutId.THUMBNAIL.id),
        )

        ExpandedTopBar(
            isScreenLocked = isScreenLocked,
            actions = actions,
            scaffoldState = scaffoldState,
            modifier = Modifier.fillMaxWidth().layoutId(LayoutId.TOP_CONTENT.id),
        )

        LockOverlay(
            isScreenLocked = isScreenLocked,
            onUnlock = { actions.onLockScreen(false) },
            modifier = Modifier.layoutId(LayoutId.LOCK_OVERLAY.id),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ExpandedTopBar(isScreenLocked: Boolean, actions: PlayerActions, scaffoldState: BottomSheetScaffoldState, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    PlayerTopBar(
        isScreenLocked = isScreenLocked,
        onCollapse = { scope.launch { scaffoldState.bottomSheetState.collapse() } },
        onLockToggle = actions.onLockScreen,
        onOpenDetails = actions.onOpenDetails,
        onMarkFinished = actions.onMarkFinished,
        onOpenSettings = actions.onOpenSettings,
        modifier = modifier,
        onOpenBookmarks = actions.onOpenBookmarks,
        onOpenHistory = actions.onOpenHistory,
        extraMenu = PlayerMenuExtras(actions.onOpenCharacters, actions.onOpenStatistics, actions.onOpenBookText, actions.onOpenText),
        routeButton = actions.routeButton,
    )
}

/** The existing MotionLayout scene of the YouTube-style player. */
@OptIn(ExperimentalMotionApi::class)
@Composable
private fun rememberPlayerMotionScene(): MotionScene {
    val context = LocalContext.current
    val content = remember { context.resources.openRawResource(R.raw.motion_scene).readBytes().decodeToString() }
    return MotionScene(content = content)
}

/** Applies the book's speed when it opens and resyncs the position when the player moves to another file. */
@Composable
private fun PlayerEffects(book: Book, playerState: PlayerState, actions: PlayerActions) {
    LaunchedEffect(book.id) { actions.onApplyBookSpeed(book.speed) }
    LaunchedEffect(playerState.currentTrackIndex) { if (playerState.playWhenReady) actions.onTrackChanged() }
}

private fun playingStateOf(playerState: PlayerState, isAwaitingPlayback: Boolean) = when {
    isAwaitingPlayback -> PlayingState.LOADING
    playerState.isPlaying && playerState.status == PlaybackStatus.READY -> PlayingState.PLAYING
    else -> PlayingState.PAUSED
}

/** The cover with its gestures and, over it while open, the chapter panel (AbbayiOS's G2 dropdown). */
@Composable
private fun CoverAndChapters(
    book: Book,
    playerIcon: PlayingState,
    skips: PlayerSkips,
    actions: PlayerActions,
    tint: Color,
    chapters: ChaptersToggle,
    gesturesEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        PlayerCover(
            thumbnail = book.thumbnail,
            playerIcon = playerIcon,
            gesturesEnabled = gesturesEnabled,
            onTap = { if (playerIcon != PlayingState.LOADING) actions.onCoverClick(book.progress) },
            onDoubleTapLeft = { actions.onSeekBy(-skips.smallBackSeconds * MILLIS_PER_SECOND) },
            onDoubleTapRight = { actions.onSeekBy(skips.smallForwardSeconds * MILLIS_PER_SECOND) },
            onSwipeToPrevious = actions.onPreviousChapter,
            onSwipeToNext = actions.onNextChapter,
        )

        AnimatedVisibility(
            visible = chapters.isOpen,
            enter = fadeIn() + scaleIn(initialScale = PANEL_INITIAL_SCALE, transformOrigin = TransformOrigin(1f, 1f)),
            exit = fadeOut() + scaleOut(targetScale = PANEL_INITIAL_SCALE, transformOrigin = TransformOrigin(1f, 1f)),
            modifier = Modifier.fillMaxSize(),
        ) {
            BackHandler(onBack = chapters.onClose)
            ChaptersPanel(
                chapters = book.chapters,
                position = PlayerPosition.of(book),
                bookTimeLeft = timeLeftLabel(book.duration - book.bookPositionMs, book.speed),
                tint = tint,
                onSelect = { chapter ->
                    chapters.onClose()
                    actions.onSeekToBookPosition(chapter.startMs)
                },
                onClose = chapters.onClose,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The chapter panel's open state, closed whenever the player collapses. */
@Composable
private fun rememberChaptersToggle(isExpanded: Boolean): ChaptersToggle {
    var isOpen by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) { if (!isExpanded) isOpen = false }
    return ChaptersToggle(isOpen = isOpen && isExpanded, onToggle = { isOpen = !isOpen }, onClose = { isOpen = false })
}

/** Locked player: a tap explains the lock, a double tap unlocks (existing AbbayAndroid behavior). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockOverlay(isScreenLocked: Boolean, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    var showUnlockDialog by remember { mutableStateOf(false) }
    if (showUnlockDialog) ScreenLockedAlert { showUnlockDialog = false }

    Box(modifier = modifier) {
        if (isScreenLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .combinedClickable(onClick = { showUnlockDialog = true }, onDoubleClick = onUnlock),
            )
        }
    }
}

private val controlsPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 30.dp)

private const val MILLIS_PER_SECOND = 1_000L
private const val PANEL_INITIAL_SCALE = 0.2f
