package com.mobyle.abbay.presentation.readalong

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.readalong.ReadAlongDisplay
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongState
import kotlinx.coroutines.delay

/**
 * The lines, or a message when there is nothing to show yet, over a bar along the bottom: the language on the
 * left and [corner] on the right. Both step aside after a few seconds without a touch, and a tap brings them
 * back, so the text has the square to itself.
 */
@Composable
internal fun ReadAlongText(
    ui: ReadAlongUi,
    style: ReadAlongStyle,
    onSeek: (Long) -> Unit,
    onSelectLanguage: (ReadAlongLanguage) -> Unit,
    corner: @Composable () -> Unit,
) {
    var showsControls by remember { mutableStateOf(true) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val hasText = ui.snapshot.state == ReadAlongState.Ready && ui.snapshot.lines.isNotEmpty()
    HideControlsWhenIdle(isVisible = showsControls && hasText && !isMenuOpen) { showsControls = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { showsControls = true }) },
    ) {
        ReadAlongBody(ui = ui, style = style, onSeek = onSeek, onInteract = { showsControls = true })
        // A scrim under the bar: the lines run to the square's bottom edge, and the chip has to stay readable.
        if (showsControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(BAR_SCRIM_HEIGHT.dp)
                    .background(
                        Brush.verticalGradient(0f to Color.Transparent, 1f to ReadAlongFill),
                    ),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = style.horizontalPadding.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showsControls) {
                ReadAlongLanguageChip(
                    language = ui.snapshot.language,
                    choices = ui.snapshot.choices,
                    onSelect = onSelectLanguage,
                    onOpenChange = { isMenuOpen = it },
                )
            }
            Box(modifier = Modifier.weight(1f))
            if (showsControls) corner()
        }
    }
}

/** How tall the scrim behind the bottom bar is. */
private const val BAR_SCRIM_HEIGHT = 84

/** Immersion: the bar steps aside once nothing has been touched for a while. */
@Composable
private fun HideControlsWhenIdle(isVisible: Boolean, hide: () -> Unit) {
    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        delay(CONTROLS_HIDE_MS)
        hide()
    }
}

@Composable
private fun ReadAlongBody(
    ui: ReadAlongUi,
    style: ReadAlongStyle,
    onSeek: (Long) -> Unit,
    onInteract: () -> Unit,
) {
    val snapshot = ui.snapshot
    val content = @Composable {
        when {
            snapshot.state != ReadAlongState.Ready || snapshot.lines.isEmpty() -> ReadAlongStatus(snapshot.state, style)
            else -> when (val display = snapshot.display) {
                is ReadAlongDisplay.Line -> ReadAlongLines(
                    lines = snapshot.lines,
                    current = display.position.line,
                    word = display.position.word,
                    isSpoken = true,
                    style = style,
                    onSeek = onSeek,
                    onInteract = onInteract,
                )

                is ReadAlongDisplay.Upcoming -> ReadAlongLines(
                    lines = snapshot.lines,
                    current = display.line,
                    word = null,
                    isSpoken = false,
                    style = style,
                    onSeek = onSeek,
                    onInteract = onInteract,
                )

                ReadAlongDisplay.Music -> ReadAlongMusic()
            }
        }
    }
    if (style.maxWidth > 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.widthIn(max = style.maxWidth.dp).fillMaxSize()) { content() }
        }
    } else {
        content()
    }
}

/** Nothing said for a long stretch: an intro, an interlude, the music after a chapter. */
@Composable
private fun ReadAlongMusic() {
    val description = stringResource(R.string.read_along_no_speech)
    Box(
        modifier = Modifier.fillMaxSize().semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = DIMMED_ALPHA),
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun ReadAlongStatus(state: ReadAlongState, style: ReadAlongStyle) {
    val isWorking = state is ReadAlongState.Preparing || state is ReadAlongState.DownloadingModel
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isWorking) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp).padding(bottom = 4.dp),
            )
        }
        Text(
            text = ReadAlongStatusMessage.of(state),
            style = if (style == ReadAlongStyle.FULL_SCREEN) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

/** What the pane says while there is no line to show. */
private object ReadAlongStatusMessage {
    @Composable
    fun of(state: ReadAlongState): String = when (state) {
        ReadAlongState.Preparing, ReadAlongState.Ready -> stringResource(R.string.read_along_preparing)
        is ReadAlongState.DownloadingModel -> stringResource(R.string.read_along_downloading, ReadAlongLanguageNames.of(state.language))
        is ReadAlongState.Unsupported -> stringResource(R.string.read_along_unsupported, ReadAlongLanguageNames.of(state.language))
        ReadAlongState.NeedsDownload -> stringResource(R.string.read_along_needs_download)
        ReadAlongState.FileMissing -> stringResource(R.string.read_along_file_missing)
        ReadAlongState.Failed -> stringResource(R.string.read_along_failed)
    }
}
