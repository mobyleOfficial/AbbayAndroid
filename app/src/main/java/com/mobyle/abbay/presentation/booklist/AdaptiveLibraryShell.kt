package com.mobyle.abbay.presentation.booklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Panes(host: LibraryPlayerHost, book: Book?, dependencies: LibraryPlayerDependencies, browsing: @Composable () -> Unit) {
    val presentation = host.presentation
    val paneWidth = (host.paneLayout as? PaneLayout.Split)?.playerWidth ?: 0.dp
    val showsPlayer = book != null && presentation.isOpen
    val showsChapters = showsPlayer && presentation.isShowingChapters && book?.chapters.orEmpty().isNotEmpty()
    val paneSheetState = rememberBottomSheetScaffoldState(bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded))
    val miniSheetState = rememberBottomSheetScaffoldState()
    val playerPane: @Composable (Modifier) -> Unit = { modifier ->
        book?.let {
            Box(modifier = modifier.fillMaxHeight()) { LibraryPlayer(it, paneSheetState, presentation, dependencies, isPane = true) }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (showsChapters) playerPane(Modifier.weight(1f))
        Box(modifier = (if (showsChapters) Modifier.width(paneWidth) else Modifier.weight(1f)).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().hiddenUnder(showsChapters)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { browsing() }
                if (book != null && !showsPlayer) {
                    val mini = Modifier.height(MINI_PLAYER_HEIGHT)
                    LibraryPlayer(book, miniSheetState, presentation, dependencies, isPane = false, modifier = mini)
                }
            }
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
        if (showsPlayer && !showsChapters) playerPane(Modifier.width(paneWidth))
    }
}

val MINI_PLAYER_HEIGHT = 72.dp
