package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.player.SleepTimerState
import com.settings.ProgressLabels

/**
 * Lower part of the expanded player: book title, chapter seek bar with elapsed and remaining
 * chapter time, a slim book progress line with time left at the current speed, the transport row and the quick actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerControls(
    book: Book,
    position: PlayerPosition,
    playerIcon: PlayingState,
    skips: PlayerSkips,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    sleepTimer: SleepTimerState = SleepTimerState.Off,
    sleepSettings: SleepSheetSettings = SleepSheetSettings(),
    effects: AudioEffectsPanel = AudioEffectsPanel(),
    options: PlaybackOptionsPanel = PlaybackOptionsPanel(),
    display: PlayerDisplay = PlayerDisplay(),
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            book.name,
            style = TextStyle(
                color = MaterialTheme.colorScheme.tertiary,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .basicMarquee()
                .fillMaxWidth(),
        )

        ChapterSeekBar(position, display.progressLabels, skips, actions)
        BookProgress(book, position, silenceSavedMs = effects.effects?.silenceSavedMs ?: 0L)
        TransportRow(book, playerIcon, skips, actions)
        QuickActionsRow(sleepTimer, sleepSettings, actions, effects = effects, options = options, hidden = display.hiddenQuickActions)
    }
}

@Composable
private fun ChapterSeekBar(position: PlayerPosition, labels: ProgressLabels, skips: PlayerSkips, actions: PlayerActions) {
    val onSeek = actions.onSeekToBookPosition
    var draggedFraction by remember { mutableStateOf<Float?>(null) }
    val chapterStart = position.chapter?.startMs ?: 0L
    val fraction = draggedFraction ?: (position.chapterElapsedMs.toFloat() / position.chapterDurationMs).coerceIn(0f, 1f)
    val elapsedMs = (fraction * position.chapterDurationMs).toLong()
    val seekLabel = stringResource(R.string.player_chapter_seek)
    val seekState = stringResource(R.string.player_seek_state, elapsedMs.toHHMMSS(), position.chapterDurationMs.toHHMMSS())
    // TalkBack users skip with actions instead of dragging.
    val skipActions = listOf(
        CustomAccessibilityAction(stringResource(R.string.player_skip_back_seconds, skips.smallBackSeconds)) {
            actions.onSeekBy(-skips.smallBackSeconds * MILLIS_PER_SECOND)
            true
        },
        CustomAccessibilityAction(stringResource(R.string.player_skip_forward_seconds, skips.smallForwardSeconds)) {
            actions.onSeekBy(skips.smallForwardSeconds * MILLIS_PER_SECOND)
            true
        },
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { draggedFraction = it },
            onValueChangeFinished = {
                draggedFraction?.let { onSeek(chapterStart + (it * position.chapterDurationMs).toLong()) }
                draggedFraction = null
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = seekLabel
                    stateDescription = seekState
                    customActions = skipActions
                },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(elapsedMs.toHHMMSS(), style = MaterialTheme.typography.titleSmall)
            val endLabel = when (labels) {
                ProgressLabels.ELAPSED_AND_REMAINING -> "-" + (position.chapterDurationMs - elapsedMs).coerceAtLeast(0L).toHHMMSS()
                ProgressLabels.ELAPSED_AND_TOTAL -> position.chapterDurationMs.toHHMMSS()
            }
            Text(endLabel, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun BookProgress(book: Book, position: PlayerPosition, silenceSavedMs: Long) {
    val fraction = progressFraction(position.bookPositionMs, book.duration)
    val progressLabel = stringResource(R.string.player_book_progress)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = progressLabel },
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeLeftLabel(book.duration - position.bookPositionMs, book.speed), style = MaterialTheme.typography.bodySmall)
            // In the same row, so the player's height does not change (the MotionLayout scene sizes the controls once).
            if (silenceSavedMs >= MILLIS_PER_MINUTE) {
                Text(
                    stringResource(R.string.effects_saved_short, (silenceSavedMs / MILLIS_PER_MINUTE).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(stringResource(R.string.player_percent, (fraction * PERCENT).toInt()), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TransportRow(
    book: Book,
    playerIcon: PlayingState,
    skips: PlayerSkips,
    actions: PlayerActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkipButton(
            Icons.Default.FastRewind,
            stringResource(R.string.player_skip_back_minutes, skips.largeBackSeconds / SECONDS_PER_MINUTE)
        ) {
            actions.onSeekBy(-skips.largeBackSeconds * MILLIS_PER_SECOND)
        }
        SkipButton(
            Icons.Default.Replay,
            stringResource(R.string.player_skip_back_seconds, skips.smallBackSeconds),
            skips.smallBackSeconds
        ) {
            actions.onSeekBy(-skips.smallBackSeconds * MILLIS_PER_SECOND)
        }
        PlayButton(playerIcon, size = PLAY_BUTTON_SIZE.dp) { actions.onPlayButtonClick(book.progress) }
        SkipButton(
            Icons.Default.Replay,
            stringResource(R.string.player_skip_forward_seconds, skips.smallForwardSeconds),
            skips.smallForwardSeconds,
            isMirrored = true,
        ) {
            actions.onSeekBy(skips.smallForwardSeconds * MILLIS_PER_SECOND)
        }
        SkipButton(
            Icons.Default.FastForward,
            stringResource(
                R.string.player_skip_forward_minutes,
                skips.largeForwardSeconds / SECONDS_PER_MINUTE
            )
        ) {
            actions.onSeekBy(skips.largeForwardSeconds * MILLIS_PER_SECOND)
        }
    }
}

/** 56 dp touch target with the amount written under small skips. */
@Composable
private fun SkipButton(icon: ImageVector, label: String, seconds: Int? = null, isMirrored: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SKIP_BUTTON_SIZE.dp)
            .repeatingClick(label, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = AbbayColors.content,
                modifier = if (isMirrored) Modifier.graphicsLayer(scaleX = -1f) else Modifier,
            )
            if (seconds != null) Text("$seconds", style = MaterialTheme.typography.labelSmall, color = AbbayColors.content)
        }
    }
}

private const val PERCENT = 100
private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val SECONDS_PER_MINUTE = 60
private const val PLAY_BUTTON_SIZE = 64
private const val SKIP_BUTTON_SIZE = 56
