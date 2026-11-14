package com.mobyle.abbay.presentation.booklist.widgets

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.models.LayoutId
import com.mobyle.abbay.presentation.booklist.widgets.player.ChaptersSheet
import com.mobyle.abbay.presentation.booklist.widgets.player.ChaptersToggle
import com.mobyle.abbay.presentation.booklist.widgets.player.MiniPlayerRow
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerActions
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerControls
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerCover
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerDisplay
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerMenuExtras
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerPosition
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerReadAlong
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerSkips
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerTopBar
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayingState
import com.mobyle.abbay.presentation.booklist.widgets.player.ReadAlongToggle
import com.mobyle.abbay.presentation.booklist.widgets.player.ScreenLockedAlert
import com.mobyle.abbay.presentation.booklist.widgets.player.SleepSheetSettings
import com.mobyle.abbay.presentation.booklist.widgets.player.chaptersToggleOf
import com.mobyle.abbay.presentation.booklist.widgets.player.playingStateOf
import com.mobyle.abbay.presentation.booklist.widgets.player.timeLeftLabel
import com.mobyle.abbay.presentation.common.theme.coverTintBackground
import com.mobyle.abbay.presentation.common.theme.rememberCoverTint
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.utils.currentFraction
import com.model.Book
import com.player.PlayerState
import kotlinx.coroutines.launch

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
    chapters: ChaptersToggle? = null,
    onClose: (() -> Unit)? = null,
    onExpand: () -> Unit = {},
    readAlong: PlayerReadAlong? = null,
) {
    val fraction = scaffoldState.currentFraction.coerceIn(0f, 1f)
    val isExpanded = fraction == 1f
    val position = PlayerPosition.of(book)
    val playerIcon = playingStateOf(playerState, isAwaitingPlayback)
    val chapterToggle = chaptersToggleOf(chapters, rememberChaptersToggle(isExpanded), isSheet = onClose == null, isExpanded)
    PlayerEffects(book, playerState, actions)
    // Only once the player is fully open: while it moves, the cover shrinks into the mini player's.
    val textInCover = readAlong?.takeIf { it.toggle.isOffered && it.toggle.showsText && isExpanded }
    var coverBounds by remember { mutableStateOf(Rect.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        MotionLayout(
            motionScene = rememberPlayerMotionScene(),
            progress = fraction,
            modifier = Modifier.fillMaxSize().coverTintBackground(rememberCoverTint(book.thumbnail), gradientStrength = fraction),
        ) {
            CollapsedRow(book, position, playerIcon, isScreenLocked, actions, onExpand, isCollapsed = fraction == 0f)

            PlayerControls(
                book = book,
                position = position,
                playerIcon = playerIcon,
                skips = skips,
                actions = actions,
                modifier = Modifier.fillMaxWidth().layoutId(LayoutId.CONTENT.id).padding(controlsPadding),
                chapters = chapterToggle,
                sleepTimer = playerState.sleepTimer,
                sleepSettings = sleepSettings,
                effects = effects,
                options = options,
                display = display,
                readAlong = readAlong?.toggle ?: ReadAlongToggle(),
            )

            Cover(
                book = book,
                playerIcon = playerIcon,
                skips = skips,
                actions = actions,
                gesturesEnabled = isExpanded && !isScreenLocked,
                isPane = onClose != null,
                modifier = Modifier.padding(8.dp).layoutId(LayoutId.THUMBNAIL.id),
                showsText = textInCover != null,
                onBounds = { coverBounds = it },
            )

            ExpandedTopBar(
                isScreenLocked = isScreenLocked,
                actions = actions,
                scaffoldState = scaffoldState,
                onClose = onClose,
                modifier = Modifier.fillMaxWidth().layoutId(LayoutId.TOP_CONTENT.id),
            )

            LockOverlay(
                isScreenLocked = isScreenLocked,
                onUnlock = { actions.onLockScreen(false) },
                modifier = Modifier.layoutId(LayoutId.LOCK_OVERLAY.id),
            )
        }
        textInCover?.let { TextOverCover(it, coverBounds, isPane = onClose != null, isScreenLocked, actions) }
    }

    if (onClose == null && chapterToggle.isOpen) PlayerChaptersSheet(book, position, chapterToggle, actions)
}

/** The mini player's row, under the expanded player while it is open. */
@Composable
private fun CollapsedRow(
    book: Book,
    position: PlayerPosition,
    playerIcon: PlayingState,
    isScreenLocked: Boolean,
    actions: PlayerActions,
    onExpand: () -> Unit,
    isCollapsed: Boolean,
) {
    MiniPlayerRow(
        book = book,
        position = position,
        playerIcon = playerIcon,
        isScreenLocked = isScreenLocked,
        onPlayButtonClick = { actions.onPlayButtonClick(book.progress) },
        onClick = onExpand,
        isScrollingLongText = isCollapsed,
        modifier = Modifier.fillMaxWidth().layoutId(LayoutId.MINI_PLAYER.id),
    )
}

@Composable
private fun PlayerChaptersSheet(book: Book, position: PlayerPosition, chapters: ChaptersToggle, actions: PlayerActions) {
    ChaptersSheet(
        chapters = book.chapters,
        position = position,
        bookTimeLeft = timeLeftLabel(book.duration - book.bookPositionMs, book.speed),
        onSelect = { chapter ->
            chapters.onClose()
            actions.onSeekToBookPosition(chapter.startMs)
        },
        onDismiss = chapters.onClose,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ExpandedTopBar(
    isScreenLocked: Boolean,
    actions: PlayerActions,
    scaffoldState: BottomSheetScaffoldState,
    onClose: (() -> Unit)?,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val collapse: () -> Unit = { scope.launch { scaffoldState.bottomSheetState.collapse() } }
    PlayerTopBar(
        isScreenLocked = isScreenLocked,
        onCollapse = onClose ?: collapse,
        onLockToggle = actions.onLockScreen,
        onOpenDetails = actions.onOpenDetails,
        onMarkFinished = actions.onMarkFinished,
        onOpenSettings = actions.onOpenSettings,
        modifier = modifier,
        onOpenBookmarks = actions.onOpenBookmarks,
        onOpenHistory = actions.onOpenHistory,
        extraMenu = PlayerMenuExtras(actions.onOpenCharacters, actions.onOpenStatistics, actions.onOpenBookText, actions.onOpenText),
        routeButton = actions.routeButton,
        isPane = onClose != null,
    )
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun rememberPlayerMotionScene(): MotionScene {
    val context = LocalContext.current
    val content = remember { context.resources.openRawResource(R.raw.motion_scene).readBytes().decodeToString() }
    return MotionScene(content = content)
}

@Composable
private fun PlayerEffects(book: Book, playerState: PlayerState, actions: PlayerActions) {
    LaunchedEffect(book.id) { actions.onApplyBookSpeed(book.speed) }
    LaunchedEffect(playerState.currentTrackIndex) { if (playerState.playWhenReady) actions.onTrackChanged() }
}

@Composable
private fun Cover(
    book: Book,
    playerIcon: PlayingState,
    skips: PlayerSkips,
    actions: PlayerActions,
    gesturesEnabled: Boolean,
    modifier: Modifier = Modifier,
    isPane: Boolean = false,
    showsText: Boolean = false,
    onBounds: (Rect) -> Unit = {},
) {
    val alignment = coverAlignment(isPane)
    // In the motion layout's coordinates, which are the player box's: it fills the box from its corner.
    Box(modifier.onGloballyPositioned { onBounds(it.boundsInParent()) }) {
        // While the text shows, the cover stays in its slot but is not seen, tapped or read out: the text is drawn
        // over it (`TextOverCover`), outside the motion layout, which kept stale sizes for content changing inside it.
        val coverModifier = if (showsText) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier
        PlayerCover(
            modifier = coverModifier,
            thumbnail = book.thumbnail,
            isStreamingOnly = isStreamingOnly(book),
            playerIcon = playerIcon,
            gesturesEnabled = gesturesEnabled && !showsText,
            onTap = { if (playerIcon != PlayingState.LOADING) actions.onCoverClick(book.progress) },
            onDoubleTapLeft = { actions.onSeekBy(-skips.smallBackSeconds * MILLIS_PER_SECOND) },
            onDoubleTapRight = { actions.onSeekBy(skips.smallForwardSeconds * MILLIS_PER_SECOND) },
            onSwipeToPrevious = actions.onPreviousChapter,
            onSwipeToNext = actions.onNextChapter,
            alignment = alignment,
        )
    }
}

@Composable
private fun rememberChaptersToggle(isExpanded: Boolean): ChaptersToggle {
    var isOpen by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) { if (!isExpanded) isOpen = false }
    return ChaptersToggle(isOpen = isOpen && isExpanded, onToggle = { isOpen = !isOpen }, onClose = { isOpen = false })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LockOverlay(isScreenLocked: Boolean, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
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
