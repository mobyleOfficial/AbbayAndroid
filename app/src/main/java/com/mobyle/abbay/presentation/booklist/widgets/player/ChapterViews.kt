package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.ProgressCapsule
import com.mobyle.abbay.presentation.utils.debounceClick
import com.model.Chapter
import kotlin.math.ceil

/**
 * The player's chapter list as a bottom sheet: a "Chapters" header with the time left in the book, and one row per
 * chapter. Chapters before the current one are finished (dimmed, with a check); the current one is in the accent on
 * a faint accent fill, with ▶, its progress and the time left in it; the others show their length. It opens scrolled
 * to the current chapter, and a tap on a row plays that chapter and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersSheet(
    chapters: List<Chapter>,
    position: PlayerPosition,
    bookTimeLeft: String,
    onSelect: (Chapter) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentIndex = position.chapter?.index ?: 0
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.player_chapters),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AbbayColors.content,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(bookTimeLeft, style = MaterialTheme.typography.bodySmall, color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA))
        }
        val listState = rememberLazyListState()
        LaunchedEffect(Unit) { listState.scrollToItem((currentIndex - 1).coerceAtLeast(0)) }
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(chapters, key = { _, chapter -> chapter.index }) { index, chapter ->
                ChapterPanelRow(
                    chapter = chapter,
                    state = when {
                        index < currentIndex -> ChapterRowState.FINISHED
                        index == currentIndex -> ChapterRowState.CURRENT
                        else -> ChapterRowState.UPCOMING
                    },
                    currentProgress = position.chapterElapsedMs.toFloat() / position.chapterDurationMs,
                    onClick = { onSelect(chapter) },
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AbbayColors.content.copy(alpha = DIVIDER_ALPHA),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

private enum class ChapterRowState { FINISHED, CURRENT, UPCOMING }

@Composable
private fun ChapterPanelRow(chapter: Chapter, state: ChapterRowState, currentProgress: Float, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    val content = AbbayColors.content
    val isCurrent = state == ChapterRowState.CURRENT
    val title = chapterTitle(chapter)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) accent.copy(alpha = CURRENT_FILL_ALPHA) else Color.Transparent)
            .debounceClick { onClick() }
            .semantics(mergeDescendants = true) { selected = isCurrent }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChapterLeadingMark(chapter, isCurrent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal),
                color = when (state) {
                    ChapterRowState.FINISHED -> content.copy(alpha = DIMMED_ALPHA)
                    ChapterRowState.CURRENT -> accent
                    ChapterRowState.UPCOMING -> content
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isCurrent) ProgressCapsule(fraction = currentProgress, fill = accent)
        }
        ChapterTrailing(chapter, state, currentProgress)
    }
}

/** The chapter's number, or ▶ on the current one. */
@Composable
private fun ChapterLeadingMark(chapter: Chapter, isCurrent: Boolean) {
    Box(modifier = Modifier.width(NUMBER_WIDTH)) {
        if (isCurrent) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.player_chapter_current),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                "${chapter.index + 1}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            )
        }
    }
}

/** A check on a finished chapter, the time left on the current one, the length on the others. */
@Composable
private fun ChapterTrailing(chapter: Chapter, state: ChapterRowState, currentProgress: Float) {
    val length = chapter.endMs - chapter.startMs
    when (state) {
        ChapterRowState.FINISHED -> Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = stringResource(R.string.player_chapter_finished),
            tint = AbbayColors.content.copy(alpha = DIMMED_ALPHA),
            modifier = Modifier.size(20.dp),
        )
        ChapterRowState.CURRENT -> Text(
            stringResource(R.string.player_time_left, chapterLengthText((length * (1f - currentProgress.coerceIn(0f, 1f))).toLong())),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.tertiary,
            maxLines = 1,
        )
        ChapterRowState.UPCOMING -> Text(
            chapterLengthText(length),
            style = MaterialTheme.typography.bodySmall,
            color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
            maxLines = 1,
        )
    }
}

/** A chapter's length in whole minutes, rounded up so a short chapter never reads "0 min". */
@Composable
private fun chapterLengthText(durationMs: Long): String {
    val totalMinutes = ceil(durationMs.coerceAtLeast(0L) / MILLIS_PER_MINUTE).toInt()
    val hours = totalMinutes / MINUTES_PER_HOUR
    return if (hours > 0) {
        stringResource(R.string.player_hours_minutes, hours, totalMinutes % MINUTES_PER_HOUR)
    } else {
        stringResource(R.string.player_minutes, totalMinutes)
    }
}

private val NUMBER_WIDTH = 28.dp

// Dimmed and secondary text keep 4.5:1 on the dark and the light sheet.
private const val SECONDARY_ALPHA = 0.7f
private const val DIMMED_ALPHA = 0.65f
private const val DIVIDER_ALPHA = 0.08f
private const val CURRENT_FILL_ALPHA = 0.12f
private const val MILLIS_PER_MINUTE = 60_000.0
private const val MINUTES_PER_HOUR = 60
