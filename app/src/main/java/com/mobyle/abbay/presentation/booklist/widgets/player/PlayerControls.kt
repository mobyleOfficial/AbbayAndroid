package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.ProgressCapsule
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Book
import com.player.SleepTimerState
import com.settings.ProgressLabels

/**
 * Lower part of the expanded player, in AbbayiOS's layout: the left-aligned title block, the chapter scrubber with
 * elapsed time, chapter name and remaining time, a slim book progress line with time left at the current speed, the
 * transport (play/pause dominant), the speed and chapters pills, and the quick actions.
 */
@Composable
fun PlayerControls(
    book: Book,
    position: PlayerPosition,
    playerIcon: PlayingState,
    skips: PlayerSkips,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    chapters: ChaptersToggle = ChaptersToggle(),
    sleepTimer: SleepTimerState = SleepTimerState.Off,
    sleepSettings: SleepSheetSettings = SleepSheetSettings(),
    effects: AudioEffectsPanel = AudioEffectsPanel(),
    options: PlaybackOptionsPanel = PlaybackOptionsPanel(),
    display: PlayerDisplay = PlayerDisplay(),
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TitleBlock(book, position)
        ChapterSeekBar(position, display.progressLabels, skips, actions)
        BookProgress(book, position, silenceSavedMs = effects.effects?.silenceSavedMs ?: 0L)
        TransportRow(book, playerIcon, skips, actions)
        PillsRow(book, position, chapters, actions)
        QuickActionsRow(sleepTimer, sleepSettings, actions, effects = effects, options = options, hidden = display.hiddenQuickActions)
    }
}

@Composable
private fun TitleBlock(book: Book, position: PlayerPosition) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            book.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = AbbayColors.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val chapter = position.chapter
        val subtitle = listOfNotNull(
            book.author?.takeIf(String::isNotBlank),
            chapter?.takeIf {
                position.chapterCount > 1
            }?.let { stringResource(R.string.player_chapter_of, it.index + 1, position.chapterCount) },
        ).joinToString(SEPARATOR)
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { draggedFraction = it },
            onValueChangeFinished = {
                draggedFraction?.let { onSeek(chapterStart + (it * position.chapterDurationMs).toLong()) }
                draggedFraction = null
            },
            interactionSource = interactionSource,
            thumb = { ScrubberThumb(isDragging = isDragging || draggedFraction != null) },
            track = {
                ProgressCapsule(
                    fraction = fraction,
                    height = TRACK_HEIGHT,
                    fill = AbbayColors.content,
                    track = AbbayColors.content.copy(alpha = TRACK_ALPHA),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = seekLabel
                    stateDescription = seekState
                    customActions = skipActions
                },
        )
        SeekLabels(elapsedMs, position, labels)
    }
}

@Composable
private fun ScrubberThumb(isDragging: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isDragging) THUMB_DRAGGED else THUMB_IDLE)
            .clip(CircleShape)
            .background(AbbayColors.content),
    )
}

@Composable
private fun SeekLabels(elapsedMs: Long, position: PlayerPosition, labels: ProgressLabels) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val labelStyle = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_FIGURES)
        val labelColor = AbbayColors.content.copy(alpha = SECONDARY_ALPHA)
        Text(elapsedMs.toHHMMSS(), style = labelStyle, color = labelColor)
        Text(
            position.chapter?.takeIf { position.chapterCount > 1 }?.let { chapterTitle(it) }.orEmpty(),
            style = labelStyle,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        val endLabel = when (labels) {
            ProgressLabels.ELAPSED_AND_REMAINING -> "−" + (position.chapterDurationMs - elapsedMs).coerceAtLeast(0L).toHHMMSS()
            ProgressLabels.ELAPSED_AND_TOTAL -> position.chapterDurationMs.toHHMMSS()
        }
        Text(endLabel, style = labelStyle, color = labelColor)
    }
}

@Composable
private fun BookProgress(book: Book, position: PlayerPosition, silenceSavedMs: Long) {
    val fraction = progressFraction(position.bookPositionMs, book.duration)
    val progressLabel = stringResource(R.string.player_book_progress)
    val labelStyle = MaterialTheme.typography.bodySmall
    val labelColor = AbbayColors.content.copy(alpha = SECONDARY_ALPHA)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        ProgressCapsule(
            fraction = fraction,
            modifier = Modifier.semantics { contentDescription = progressLabel },
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeLeftLabel(book.duration - position.bookPositionMs, book.speed), style = labelStyle, color = labelColor)
            // In the same row, so the player's height does not change (the MotionLayout scene sizes the controls once).
            if (silenceSavedMs >= MILLIS_PER_MINUTE) {
                Text(
                    stringResource(R.string.effects_saved_short, (silenceSavedMs / MILLIS_PER_MINUTE).toInt()),
                    style = labelStyle,
                    color = labelColor,
                )
            }
            Text(stringResource(R.string.player_percent, (fraction * PERCENT).toInt()), style = labelStyle, color = labelColor)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LargeSkipButton(
            Icons.Rounded.FastRewind,
            stringResource(R.string.player_skip_back_minutes, skips.largeBackSeconds / SECONDS_PER_MINUTE)
        ) {
            actions.onSeekBy(-skips.largeBackSeconds * MILLIS_PER_SECOND)
        }
        SkipButton(stringResource(R.string.player_skip_back_seconds, skips.smallBackSeconds), skips.smallBackSeconds) {
            actions.onSeekBy(-skips.smallBackSeconds * MILLIS_PER_SECOND)
        }
        PlayButton(playerIcon, size = PLAY_BUTTON_SIZE.dp, filled = true) { actions.onPlayButtonClick(book.progress) }
        SkipButton(
            stringResource(R.string.player_skip_forward_seconds, skips.smallForwardSeconds),
            skips.smallForwardSeconds,
            isForward = true,
        ) {
            actions.onSeekBy(skips.smallForwardSeconds * MILLIS_PER_SECOND)
        }
        LargeSkipButton(
            Icons.Rounded.FastForward,
            stringResource(R.string.player_skip_forward_minutes, skips.largeForwardSeconds / SECONDS_PER_MINUTE)
        ) {
            actions.onSeekBy(skips.largeForwardSeconds * MILLIS_PER_SECOND)
        }
    }
}

@Composable
private fun SkipButton(label: String, seconds: Int, isForward: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SKIP_BUTTON_SIZE.dp)
            .repeatingClick(label, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Replay,
            contentDescription = null,
            tint = AbbayColors.content,
            modifier = Modifier
                .size(SKIP_ICON_SIZE.dp)
                .graphicsLayer(scaleX = if (isForward) -1f else 1f),
        )
        Text(
            "$seconds",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = SKIP_NUMBER_SIZE.sp),
            color = AbbayColors.content,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun LargeSkipButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SKIP_BUTTON_SIZE.dp)
            .repeatingClick(label, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AbbayColors.content.copy(alpha = LARGE_SKIP_ALPHA), modifier = Modifier.size(26.dp))
    }
}

private const val PERCENT = 100
private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val SECONDS_PER_MINUTE = 60
private const val PLAY_BUTTON_SIZE = 68
private const val SKIP_BUTTON_SIZE = 56
private const val SKIP_ICON_SIZE = 36
private const val SKIP_NUMBER_SIZE = 10
private const val LARGE_SKIP_ALPHA = 0.8f
private const val SECONDARY_ALPHA = 0.7f
private const val TRACK_ALPHA = 0.2f
private const val SEPARATOR = " · "
internal const val TABULAR_FIGURES = "tnum"
private val TRACK_HEIGHT = 6.dp
private val THUMB_IDLE = 12.dp
private val THUMB_DRAGGED = 18.dp
