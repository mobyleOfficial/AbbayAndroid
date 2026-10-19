package com.mobyle.abbay.presentation.history

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.HistoryEvent
import com.model.HistoryEventType

/**
 * History of a book, newest first; tapping an entry offers "Go to this
 * position" — the place before a move, which is what a pocket press or falling asleep takes away.
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel(), onJump: (Long) -> Unit, close: () -> Unit) {
    val events by viewModel.events.collectAsState()
    var selected by remember { mutableStateOf<HistoryEvent?>(null) }

    selected?.let { event ->
        val position = event.positionMs.toHHMMSS()
        AbbayAlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(stringResource(R.string.history_go_to_title)) },
            text = { Text(stringResource(R.string.history_go_to_body, position)) },
            confirmButton = {
                AbbayTextButton(onClick = {
                    selected = null
                    onJump(event.positionMs)
                }) { Text(stringResource(R.string.history_go_to)) }
            },
            dismissButton = { AbbayTextButton(onClick = { selected = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    AbbayScreen(title = stringResource(R.string.history_title), onClose = close) {
        val list = events ?: return@AbbayScreen
        if (list.isEmpty()) {
            EmptyMessage(stringResource(R.string.history_empty))
            return@AbbayScreen
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { event -> HistoryRow(event) { selected = event } }
        }
    }
}

@Composable
private fun HistoryRow(event: HistoryEvent, onClick: () -> Unit) {
    val context = LocalContext.current
    val time = DateUtils.formatDateTime(
        context,
        event.at,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = stringResource(R.string.history_go_to_title), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(event.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(description(event), style = MaterialTheme.typography.bodyLarge, color = AbbayColors.content)
            Text(time, style = MaterialTheme.typography.bodySmall, color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA))
        }
    }
}

@Composable
private fun description(event: HistoryEvent): String {
    val position = event.positionMs.toHHMMSS()
    val target = (event.targetPositionMs ?: event.positionMs).toHHMMSS()
    val at = stringResource(R.string.history_at, position)
    return when (event.type) {
        HistoryEventType.PLAY -> "${stringResource(R.string.history_play)} · $at"
        HistoryEventType.PAUSE -> "${stringResource(R.string.history_pause)} · $at"
        HistoryEventType.SEEK -> stringResource(R.string.history_seek, position, target)
        HistoryEventType.CHAPTER_JUMP -> stringResource(R.string.history_chapter_jump, position, target)
        HistoryEventType.SLEEP_TIMER_STOP -> "${stringResource(R.string.history_sleep_stop)} · $at"
        HistoryEventType.SLEEP_TIMER_EXTEND -> "${stringResource(R.string.history_sleep_extend)} · $at"
        HistoryEventType.BOOKMARK_ADDED -> "${stringResource(R.string.history_bookmark)} · $at"
        HistoryEventType.FINISHED -> stringResource(R.string.history_finished)
    }
}

private fun HistoryEventType.icon(): ImageVector = when (this) {
    HistoryEventType.PLAY -> Icons.Default.PlayArrow
    HistoryEventType.PAUSE -> Icons.Default.Pause
    HistoryEventType.SEEK -> Icons.Default.SwapHoriz
    HistoryEventType.CHAPTER_JUMP -> Icons.Default.SkipNext
    HistoryEventType.SLEEP_TIMER_STOP -> Icons.Default.Bedtime
    HistoryEventType.SLEEP_TIMER_EXTEND -> Icons.Default.Vibration
    HistoryEventType.BOOKMARK_ADDED -> Icons.Default.Bookmark
    HistoryEventType.FINISHED -> Icons.Default.DoneAll
}

private const val SECONDARY_ALPHA = 0.7f
