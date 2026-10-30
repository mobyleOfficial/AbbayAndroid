package com.mobyle.abbay.presentation.booklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.booklist.widgets.player.ChaptersPane
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayerPosition
import com.mobyle.abbay.presentation.booklist.widgets.player.timeLeftLabel
import com.mobyle.abbay.presentation.documents.CompanionDocumentsDialog
import com.model.Book

/**
 * The app's screens ([content]) as browsing, with the player beside them when the room the app is given fits both
 * panes (`AdaptiveLayout`). While the chapters are open the player moves to the leading side, taking the larger
 * share, and the chapters take browsing's place at the pane width. Browsing keeps its slot in every state, so it is
 * never rebuilt: under the chapters it is only hidden, and closing them returns to the same screen and scroll.
 *
 * In one pane this adds nothing: the library screen owns the player as its bottom sheet. Split, the player lives
 * here, so Search, LibriVox, the queue or Settings open beside it, and while it is closed a mini player sits under
 * them.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AdaptiveLibraryShell(dependencies: LibraryPlayerDependencies, content: @Composable (LibraryPlayerHost) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val paneLayout = AdaptiveLayout.decide(maxWidth.value, maxHeight.value, LocalDensity.current.fontScale)
        val sheetState = rememberBottomSheetScaffoldState()
        val presentation = rememberPlayerPresentation(sheetState.bottomSheetState, paneLayout)
        val host = remember(presentation, sheetState) { LibraryPlayerHost(presentation, sheetState) }
        val viewModel = dependencies.viewModel
        val selectedBook by viewModel.selectedBook.collectAsState()

        LaunchedEffect(selectedBook?.id) {
            dependencies.effectsViewModel.load(selectedBook?.id)
            dependencies.optionsViewModel.load(selectedBook?.id?.takeUnless { selectedBook?.isExternal == true })
        }
        LaunchedEffect(selectedBook == null) {
            if (selectedBook == null && presentation.isOpen) presentation.close()
        }
        CompanionDocumentsDialog(dependencies.documentsViewModel)

        // One tree in every state, so the screens (and their navigation) are never rebuilt by a fold or a resize.
        Panes(host, book = selectedBook?.takeIf { paneLayout.isSplit }, dependencies = dependencies) { content(host) }
    }
}

/** What the panes show; the player slides in or out only between [CLOSED] and [PLAYER]. */
private enum class PaneState { CLOSED, PLAYER, CHAPTERS }

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Panes(host: LibraryPlayerHost, book: Book?, dependencies: LibraryPlayerDependencies, browsing: @Composable () -> Unit) {
    val presentation = host.presentation
    val paneWidth = (host.paneLayout as? PaneLayout.Split)?.playerWidth ?: 0.dp
    val showsPlayer = book != null && presentation.isOpen
    val showsChapters = showsPlayer && presentation.isShowingChapters && book?.chapters.orEmpty().isNotEmpty()
    val reveal = playerPaneReveal(paneStateOf(showsPlayer = showsPlayer, showsChapters = showsChapters))
    // Read while composing only when the pane shows or hides, and when browsing gives it room: the frames of the
    // slide only move pixels, since re-measuring the screens beside it on every frame drops frames.
    val isPaneShown by remember(reveal) { derivedStateOf { reveal.value > 0f } }
    val reservesPane by remember(reveal) { derivedStateOf { reveal.value >= 1f } }

    val paneSheetState = rememberBottomSheetScaffoldState(bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded))
    val miniSheetState = rememberBottomSheetScaffoldState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (showsChapters) PlayerPane(book, paneSheetState, presentation, dependencies, Modifier.weight(1f))
                Box(modifier = (if (showsChapters) Modifier.width(paneWidth) else Modifier.weight(1f)).fillMaxHeight()) {
                    Box(modifier = Modifier.fillMaxSize().hiddenUnder(showsChapters)) { browsing() }
                    if (showsChapters) {
                        ChaptersPane(
                            chapters = book.chapters,
                            position = PlayerPosition.of(book),
                            bookTimeLeft = timeLeftLabel(book.duration - book.bookPositionMs, book.speed),
                            onSelect = { chapter -> dependencies.viewModel.seekToBookPosition(chapter.startMs) },
                            onClose = { presentation.setChapters(open = false) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                // Browsing narrows once the pane has slid over it, and widens again as soon as it starts to leave.
                if (reservesPane) Spacer(modifier = Modifier.width(paneWidth))
            }
            // Under the whole row, not inside browsing: it only shows while the player is closed (browsing at full
            // width), so its width stays the same while the pane slides.
            AnimatedVisibility(visible = book != null && !showsPlayer, enter = miniPlayerEnter, exit = miniPlayerExit) {
                book?.let {
                    val mini = Modifier.height(MINI_PLAYER_HEIGHT)
                    LibraryPlayer(it, miniSheetState, presentation, dependencies, isPane = false, modifier = mini)
                }
            }
        }
        // Full height, over the mini player's row, so its size never changes as the mini player leaves or returns.
        // Kept composed off the trailing edge while closed, so opening it only moves pixels; silent while hidden.
        if (book != null && !showsChapters) {
            PlayerPane(
                book = book,
                sheetState = paneSheetState,
                presentation = presentation,
                dependencies = dependencies,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(paneWidth)
                    .graphicsLayer { translationX = (1f - reveal.value) * size.width }
                    .hiddenUnder(!isPaneShown),
            )
        }
    }
}

private fun paneStateOf(showsPlayer: Boolean, showsChapters: Boolean) = when {
    showsChapters -> PaneState.CHAPTERS
    showsPlayer -> PaneState.PLAYER
    else -> PaneState.CLOSED
}

/** How much of the trailing player pane is shown. The chapters swap the player's side, so that change is instant. */
@Composable
private fun playerPaneReveal(state: PaneState): State<Float> =
    updateTransition(state, label = "panes").animateFloat(
        transitionSpec = {
            val swapsSides = initialState == PaneState.CHAPTERS || targetState == PaneState.CHAPTERS
            if (swapsSides) snap() else tween(PANE_SLIDE_MS, easing = FastOutSlowInEasing)
        },
        label = "playerPane",
    ) { if (it == PaneState.PLAYER) 1f else 0f }

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PlayerPane(
    book: Book?,
    sheetState: BottomSheetScaffoldState,
    presentation: PlayerPresentation,
    dependencies: LibraryPlayerDependencies,
    modifier: Modifier,
) {
    book ?: return
    Box(modifier = modifier.fillMaxHeight()) { LibraryPlayer(book, sheetState, presentation, dependencies, isPane = true) }
}

private const val PANE_SLIDE_MS = 320

/** The mini player drops away as the pane slides in, and rises as it leaves; its size never animates (see `Panes`). */
private val miniPlayerEnter = slideInVertically(tween(PANE_SLIDE_MS)) { it } + fadeIn(tween(PANE_SLIDE_MS))
private val miniPlayerExit = slideOutVertically(tween(PANE_SLIDE_MS)) { it } + fadeOut(tween(PANE_SLIDE_MS))

val MINI_PLAYER_HEIGHT = 72.dp
