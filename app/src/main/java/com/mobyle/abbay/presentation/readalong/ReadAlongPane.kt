package com.mobyle.abbay.presentation.readalong

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.coverCornerRadius
import com.model.Book

/**
 * The read-along text in the player's cover square: the text takes the cover's place, like lyrics, and nothing
 * else on the player moves. Its expand button shows the same text over the whole window ([ReadAlongFullScreen]).
 *
 * The session runs only while this is on screen: [ReadAlongViewModel.acquire] when it appears and `release` when
 * it goes, so nothing is transcribed while the text is hidden.
 */
@Composable
fun ReadAlongPane(
    book: Book,
    viewModel: ReadAlongViewModel,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(book.id) { viewModel.acquire(book) }
    DisposableEffect(book.id) { onDispose { viewModel.release() } }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .size(side)
                .clip(RoundedCornerShape(coverCornerRadius(side)))
                .background(ReadAlongFill),
        ) {
            ReadAlongText(
                ui = ui,
                style = ReadAlongStyle.SQUARE,
                onSeek = viewModel::seekTo,
                onSelectLanguage = viewModel::selectLanguage,
                corner = {
                    ReadAlongCornerButton(
                        icon = Icons.Rounded.Fullscreen,
                        label = stringResource(R.string.read_along_full_screen),
                        onClick = onExpand,
                    )
                },
            )
        }
    }
}

/** The same text over the whole window, larger, in a column narrow enough to read on a tablet. */
@Composable
fun ReadAlongFullScreen(book: Book, viewModel: ReadAlongViewModel, onClose: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    LaunchedEffect(book.id) { viewModel.acquire(book) }
    DisposableEffect(book.id) { onDispose { viewModel.release() } }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(ReadAlongFullScreenBackground)) {
            ReadAlongText(
                ui = ui,
                style = ReadAlongStyle.FULL_SCREEN,
                onSeek = viewModel::seekTo,
                onSelectLanguage = viewModel::selectLanguage,
                corner = {
                    ReadAlongCornerButton(
                        icon = Icons.Rounded.Close,
                        label = stringResource(R.string.read_along_exit_full_screen),
                        onClick = onClose,
                    )
                },
            )
        }
    }
}
