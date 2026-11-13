package com.mobyle.abbay.presentation.readalong

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.readalong.ReadAlongDisplay
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongSnapshot
import com.readalong.ReadAlongState
import kotlinx.coroutines.delay

/**
 * The lines, or a message while there is nothing to show yet, over a bar along the bottom: the language label on the
 * left; on the right the sync adjustment, once there is text, and [trailing] (expand, or collapse); [center] between
 * them (the full screen's play/pause).
 *
 * While the line shown was generated from the audio (no EPUB, or a passage it doesn't match), a note at the top says
 * so, and that it can be wrong; it stays when the controls hide.
 *
 * The bar hides after [CONTROLS_HIDE_DELAY_MS] without interaction, for immersion; a tap anywhere brings it back (the
 * first tap only does that), as does scrolling the lines. It stays while it may be needed: with TalkBack on, while
 * a menu is open, and while the text isn't ready (the label is how another language is picked).
 */
@Composable
fun ReadAlongContent(
    snapshot: ReadAlongSnapshot,
    timeMs: Long,
    style: ReadAlongStyle,
    onLineTap: (ReadAlongLine) -> Unit,
    onLanguagePick: (String) -> Unit,
    sync: ReadAlongSyncControls,
    bookText: ReadAlongBookTextControls,
    modelDownload: ReadAlongModelDownloadControls,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val isTalkBackOn = rememberIsTouchExplorationOn()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isSyncOpen by remember { mutableStateOf(false) }
    var interactions by remember { mutableIntStateOf(0) }
    var areControlsHidden by remember { mutableStateOf(false) }
    val isPinned = isTalkBackOn || isMenuOpen || isSyncOpen || snapshot.state != ReadAlongState.Ready
    LaunchedEffect(interactions, isPinned) {
        areControlsHidden = false
        if (!isPinned) {
            delay(CONTROLS_HIDE_DELAY_MS)
            areControlsHidden = true
        }
    }
    val reveal = { interactions += 1 }
    val hiddenState by rememberUpdatedHidden(areControlsHidden)

    Box(modifier = modifier.fillMaxSize().revealsControls(isHidden = { hiddenState }, reveal = reveal)) {
        Box(modifier = Modifier.fillMaxSize()) {
            ShownText(snapshot, timeMs, style, onLineTap, sync.onAlign, modelDownload, onScroll = reveal)
        }
        GeneratedTextNotice(snapshot, timeMs, modifier = Modifier.align(Alignment.TopCenter))
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                LanguageMenu(
                    snapshot = snapshot,
                    isHidden = areControlsHidden,
                    isOpen = isMenuOpen,
                    onOpenChange = { isMenuOpen = it },
                    onPick = onLanguagePick,
                    bookText = bookText,
                )
            }
            Box { FadingControl(visible = !areControlsHidden, content = center) }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only over text: there is nothing to line up before it.
                if (snapshot.state == ReadAlongState.Ready && snapshot.transcript != null) {
                    SyncMenu(sync, isHidden = areControlsHidden, isOpen = isSyncOpen, onOpenChange = { isSyncOpen = it })
                }
                FadingControl(visible = !areControlsHidden, content = trailing)
            }
        }
    }
}

/** Any touch brings the controls back; while they are hidden, the first one only does that: it doesn't seek. */
private fun Modifier.revealsControls(isHidden: () -> Boolean, reveal: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.any { it.pressed && !it.previousPressed }) {
                if (isHidden()) event.changes.forEach { it.consume() }
                reveal()
            }
        }
    }
}

/** Left out while hidden (not merely transparent), so a hidden control can't be tapped or focused. */
@Composable
private fun FadingControl(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) { content() }
}

@Composable
private fun rememberUpdatedHidden(value: Boolean) = remember { mutableStateOf(value) }.apply { this.value = value }

@Suppress("LongParameterList")
@Composable
private fun ShownText(
    snapshot: ReadAlongSnapshot,
    timeMs: Long,
    style: ReadAlongStyle,
    onLineTap: (ReadAlongLine) -> Unit,
    onLineHold: (ReadAlongLine) -> Unit,
    modelDownload: ReadAlongModelDownloadControls,
    onScroll: () -> Unit,
) {
    val transcript = snapshot.transcript
    if (snapshot.state != ReadAlongState.Ready || transcript == null) {
        Status(snapshot.state, style, modelDownload)
        return
    }
    // With animations off, only the line: a word lighting up every half second is motion too.
    val reducesMotion = rememberReducesMotion()
    when (val display = transcript.display(timeMs)) {
        is ReadAlongDisplay.Line -> {
            val word = if (reducesMotion) null else display.position.word
            Lines(
                transcript.lines, display.position.line, isSpoken = true, word, style, onLineTap, onLineHold, onScroll,
                animates = !reducesMotion,
            )
        }
        // An intro or a pause: the next line waits in the middle, dimmed, and lights up in place when it is read.
        is ReadAlongDisplay.Upcoming ->
            Lines(
                transcript.lines, display.line, isSpoken = false, word = null, style, onLineTap, onLineHold, onScroll,
                animates = !reducesMotion,
            )
        ReadAlongDisplay.Music -> Music()
    }
}

/** *AI-generated text · may contain mistakes* over the top of the text, while the line shown is the transcription's. */
@Composable
private fun GeneratedTextNotice(snapshot: ReadAlongSnapshot, timeMs: Long, modifier: Modifier = Modifier) {
    val transcript = snapshot.transcript
    if (snapshot.state != ReadAlongState.Ready || transcript == null) return
    val line = when (val display = transcript.display(timeMs)) {
        is ReadAlongDisplay.Line -> transcript.lines[display.position.line]
        is ReadAlongDisplay.Upcoming -> transcript.lines[display.line]
        ReadAlongDisplay.Music -> null
    }
    if (line == null || line.fromBook) return
    Row(
        modifier = modifier
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = NOTICE_FILL_ALPHA))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.read_along_generated_notice),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 2,
        )
    }
}

/** ♪ for a long stretch without speech: a sentence there read as an error. TalkBack still says *No speech here*. */
@Composable
private fun Music() {
    val description = stringResource(R.string.read_along_no_speech)
    Box(
        modifier = Modifier.fillMaxSize().semantics(mergeDescendants = true) {
            contentDescription = description
        },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = MUSIC_ALPHA),
            modifier = Modifier.size(56.dp)
        )
    }
}

/**
 * Why there is no text to show yet, centred, with a spinner while something is under way. While the speech model
 * downloads it can be cancelled, and once cancelled, downloaded again.
 */
@Composable
private fun Status(state: ReadAlongState, style: ReadAlongStyle, modelDownload: ReadAlongModelDownloadControls) {
    val message = statusMessage(state)
    val progress = (state as? ReadAlongState.DownloadingModel)?.progress
    val isWorking = state == ReadAlongState.Preparing || state == ReadAlongState.Ready || state is ReadAlongState.DownloadingModel
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).semantics(mergeDescendants = true) {
            if (progress != null) stateDescription = "${(progress * PERCENT).toInt()}%"
        },
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isWorking) {
            if (progress != null) {
                CircularProgressIndicator(progress = { progress }, color = Color.White, trackColor = Color.White.copy(alpha = TRACK_ALPHA))
            } else {
                CircularProgressIndicator(color = Color.White)
            }
        }
        Text(
            message,
            style = if (style == ReadAlongStyle.SQUARE) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        ModelDownloadButton(state, modelDownload)
    }
}

@Composable
private fun statusMessage(state: ReadAlongState): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (state) {
        ReadAlongState.Preparing, ReadAlongState.Ready -> stringResource(R.string.read_along_preparing)
        is ReadAlongState.DownloadingModel ->
            stringResource(R.string.read_along_downloading_model, ReadAlongLanguage.displayName(state.locale, locale))
        is ReadAlongState.ModelDownloadCancelled ->
            stringResource(R.string.read_along_model_download_cancelled, ReadAlongLanguage.displayName(state.locale, locale))
        is ReadAlongState.Unsupported -> stringResource(
            R.string.read_along_unsupported,
            ReadAlongLanguage.displayName(state.locale, locale)
        )
        ReadAlongState.NeedsDownload -> stringResource(R.string.read_along_needs_download)
        ReadAlongState.FileMissing -> stringResource(R.string.read_along_file_missing)
        ReadAlongState.Failed -> stringResource(R.string.read_along_failed)
    }
}

/** *Cancel download* while the speech model downloads; *Download again* once the listener cancelled it. */
@Composable
private fun ModelDownloadButton(state: ReadAlongState, modelDownload: ReadAlongModelDownloadControls) {
    val (label, onClick) = when (state) {
        is ReadAlongState.DownloadingModel -> R.string.read_along_cancel_model_download to modelDownload.onCancel
        is ReadAlongState.ModelDownloadCancelled -> R.string.read_along_retry_model_download to modelDownload.onRetry
        else -> return
    }
    TextButton(onClick = onClick) {
        Text(stringResource(label), color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

const val CONTROLS_HIDE_DELAY_MS = 3_000L
private const val MUSIC_ALPHA = 0.7f
private const val NOTICE_FILL_ALPHA = 0.55f
private const val TRACK_ALPHA = 0.3f
private const val PERCENT = 100
