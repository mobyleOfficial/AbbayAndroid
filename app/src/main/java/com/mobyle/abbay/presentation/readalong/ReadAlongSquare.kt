package com.mobyle.abbay.presentation.readalong

import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.player.PlayingState
import com.mobyle.abbay.presentation.booklist.widgets.player.label
import com.mobyle.abbay.presentation.booklist.widgets.player.toIcon
import com.mobyle.abbay.presentation.common.theme.coverTintBackground
import com.mobyle.abbay.presentation.common.theme.isDarkTheme
import com.mobyle.abbay.presentation.common.theme.rememberCoverTint
import com.mobyle.abbay.presentation.common.widgets.coverCornerRadius
import com.model.Book

/**
 * The read-along text in the player's cover square: the text takes the cover's place, like lyrics, and nothing else
 * on the player moves. Its expand button shows the same text over the whole window.
 */
@Composable
fun ReadAlongSquare(
    book: Book,
    viewModel: ReadAlongViewModel,
    playback: ReadAlongPlayback,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    ReadAlongOnScreen(book, viewModel)
    val snapshot by viewModel.snapshot.collectAsState()
    val timeMs by viewModel.textTimeMs.collectAsState()
    val sync = rememberSyncControls(viewModel)
    var fullScreenBookId by rememberSaveable { mutableStateOf<String?>(null) }
    val fill = squareFill()
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = alignment) {
        val side = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .size(side)
                .clip(RoundedCornerShape(coverCornerRadius(side)))
                .background(fill),
        ) {
            ReadAlongContent(
                snapshot = snapshot,
                timeMs = timeMs,
                style = ReadAlongStyle.SQUARE,
                onLineTap = { line -> playback.onSeekToBookPosition(viewModel.sync.audioTimeOf(line)) },
                onLanguagePick = viewModel::setLanguage,
                sync = sync,
                trailing = {
                    ReadAlongCornerButton(Icons.Rounded.OpenInFull, stringResource(R.string.read_along_full_screen)) {
                        fullScreenBookId = book.id
                    }
                },
            )
        }
    }
    // Tied to the book it opened on: when another book loads, its text would be the other book's.
    if (fullScreenBookId == book.id) {
        ReadAlongFullScreen(book, viewModel, playback, onClose = { fullScreenBookId = null })
    } else if (fullScreenBookId != null) {
        LaunchedEffect(book.id) { fullScreenBookId = null }
    }
}

/**
 * The read-along text over the whole window, on the player's cover tint: the same session, lines and highlight as the
 * square, larger, in a centred column, over a bar with the language, play/pause and the collapse button. Back closes it.
 */
@Composable
private fun ReadAlongFullScreen(book: Book, viewModel: ReadAlongViewModel, playback: ReadAlongPlayback, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        EdgeToEdgeDialogWindow()
        ReadAlongOnScreen(book, viewModel)
        val snapshot by viewModel.snapshot.collectAsState()
        val timeMs by viewModel.textTimeMs.collectAsState()
        val sync = rememberSyncControls(viewModel)
        val title = stringResource(R.string.read_along_toggle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .coverTintBackground(rememberCoverTint(book.thumbnail))
                .background(squareFill())
                .semantics { paneTitle = title }
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            ReadAlongContent(
                snapshot = snapshot,
                timeMs = timeMs,
                style = ReadAlongStyle.FULL_SCREEN,
                onLineTap = { line -> playback.onSeekToBookPosition(viewModel.sync.audioTimeOf(line)) },
                onLanguagePick = viewModel::setLanguage,
                sync = sync,
                center = { PlayPause(playback) },
                trailing = {
                    ReadAlongCornerButton(Icons.Rounded.CloseFullscreen, stringResource(R.string.read_along_exit_full_screen), onClose)
                },
            )
        }
    }
}

/** The tint reaches every edge, under clear system bars with light icons; the text and the bar keep inside the safe area. */
@Composable
private fun EdgeToEdgeDialogWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
        )
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

@Composable
private fun PlayPause(playback: ReadAlongPlayback) {
    val icon = playback.playerIcon.toIcon()
    val label = playback.playerIcon.label()
    IconButton(
        onClick = { if (playback.playerIcon != PlayingState.LOADING) playback.onPlayPause() },
        modifier = Modifier.size(56.dp).semantics { contentDescription = label },
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        } else {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun rememberSyncControls(viewModel: ReadAlongViewModel): ReadAlongSyncControls {
    val offsetMs by viewModel.sync.offsetMs.collectAsState()
    return remember(viewModel, offsetMs) {
        ReadAlongSyncControls(offsetMs, viewModel.sync::nudge, viewModel.sync::reset, viewModel::alignTo)
    }
}

/** The square's dark fill under the white text: light over a dark tint, stronger over a light theme's pale tint. */
@Composable
private fun squareFill(): Color = Color.Black.copy(alpha = if (isDarkTheme) DARK_FILL_ALPHA else LIGHT_FILL_ALPHA)

/**
 * Counts this view in while it is on screen and the app is in the foreground, and out when it leaves either, so the
 * session transcribes only while someone can see the text. The book it follows may change while it stays.
 */
@Composable
private fun ReadAlongOnScreen(book: Book, viewModel: ReadAlongViewModel) {
    val currentBook by rememberUpdatedState(book)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        var isShown = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!isShown) {
                    isShown = true
                    viewModel.show(currentBook)
                }
                Lifecycle.Event.ON_STOP -> if (isShown) {
                    isShown = false
                    viewModel.hide()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (isShown) viewModel.hide()
        }
    }
    LaunchedEffect(book) { viewModel.follow(book) }
}

private const val DARK_FILL_ALPHA = 0.3f
private const val LIGHT_FILL_ALPHA = 0.62f
