package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.effects.AudioEffectsPanel
import com.mobyle.abbay.presentation.effects.AudioEffectsSheet
import com.mobyle.abbay.presentation.options.PlaybackOptionsPanel
import com.mobyle.abbay.presentation.options.RepeatSheet
import com.mobyle.abbay.presentation.options.SkipSheet
import com.mobyle.abbay.presentation.options.labelResource
import com.player.PlaybackOptions
import com.player.RepeatMode
import com.player.SleepTimerState
import com.settings.QuickAction

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsRow(
    sleepTimer: SleepTimerState,
    sleepSettings: SleepSheetSettings,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    effects: AudioEffectsPanel = AudioEffectsPanel(),
    options: PlaybackOptionsPanel = PlaybackOptionsPanel(),
    hidden: Set<QuickAction> = emptySet(),
) {
    var showRepeatSheet by remember { mutableStateOf(false) }
    var showSkipSheet by remember { mutableStateOf(false) }
    if (showRepeatSheet) RepeatSheet(options) { showRepeatSheet = false }
    if (showSkipSheet) SkipSheet(options) { showSkipSheet = false }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    if (showEffectsSheet) AudioEffectsSheet(effects) { showEffectsSheet = false }
    if (showSleepSheet) {
        SleepTimerSheet(
            state = sleepTimer,
            settings = sleepSettings,
            onStart = actions.onStartSleepTimer,
            onCancel = actions.onCancelSleepTimer,
            onShakeSensitivityChange = actions.onShakeSensitivityChange,
            onDismiss = { showSleepSheet = false },
        )
    }

    val running = sleepTimer as? SleepTimerState.Running
    val sleepLabel = running?.let { formatCountdown(it.remainingMs) } ?: stringResource(R.string.quick_sleep)
    val sleepDescription = running?.let { stringResource(R.string.player_sleep_timer_running, formatCountdown(it.remainingMs)) }
        ?: stringResource(R.string.player_sleep_timer)

    val actionsPerRow = if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) ACTIONS_PER_ROW_WITH_LARGE_TEXT else Int.MAX_VALUE
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        maxItemsInEachRow = actionsPerRow,
    ) {
        if (QuickAction.SLEEP_TIMER !in hidden) {
            QuickAction(
                icon = if (running != null) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                label = sleepLabel,
                description = sleepDescription,
                isActive = running != null,
                onClick = { showSleepSheet = true },
            )
        }
        if (QuickAction.BOOKMARK !in hidden) {
            QuickAction(
                icon = Icons.Filled.BookmarkAdd,
                label = stringResource(R.string.quick_bookmark),
                description = stringResource(R.string.player_add_bookmark),
                isActive = false,
                onClick = actions.onAddBookmark,
            )
        }
        if (QuickAction.AUDIO_EFFECTS !in hidden) {
            QuickAction(
                icon = Icons.Filled.GraphicEq,
                label = stringResource(R.string.player_effects_short),
                description = stringResource(R.string.player_audio_effects),
                isActive = false,
                onClick = { showEffectsSheet = true },
            )
        }
        RepeatAndSkipActions(options.options, hidden, onRepeat = { showRepeatSheet = true }, onSkip = { showSkipSheet = true })
    }
}

@Composable
private fun RowScope.RepeatAndSkipActions(options: PlaybackOptions, hidden: Set<QuickAction>, onRepeat: () -> Unit, onSkip: () -> Unit) {
    if (QuickAction.REPEAT !in hidden) RepeatAction(options, onRepeat)
    if (QuickAction.SKIP_INTRO_OUTRO !in hidden) SkipAction(options, onSkip)
}

@Composable
private fun RowScope.RepeatAction(options: PlaybackOptions, onRepeat: () -> Unit) {
    val repeat = options.repeatMode
    val repeatName = stringResource(repeat.labelResource())
    QuickAction(
        icon = if (repeat == RepeatMode.TRACK || repeat == RepeatMode.CHAPTER) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
        label = stringResource(R.string.quick_repeat),
        description = if (repeat ==
            RepeatMode.OFF
        ) {
            stringResource(R.string.repeat_title)
        } else {
            stringResource(R.string.quick_repeat_on, repeatName)
        },
        isActive = repeat != RepeatMode.OFF,
        onClick = onRepeat,
    )
}

@Composable
private fun RowScope.SkipAction(options: PlaybackOptions, onSkip: () -> Unit) {
    val intro = options.skipIntroSeconds
    val outro = options.skipOutroSeconds
    QuickAction(
        icon = Icons.Filled.ContentCut,
        label = stringResource(R.string.quick_skip),
        description = if (intro + outro ==
            0
        ) {
            stringResource(R.string.skip_title)
        } else {
            stringResource(R.string.quick_skip_on, intro, outro)
        },
        isActive = intro + outro > 0,
        onClick = onSkip,
    )
}

@Composable
private fun RowScope.QuickAction(icon: ImageVector, label: String, description: String, isActive: Boolean, onClick: () -> Unit) {
    val color = if (isActive) MaterialTheme.colorScheme.tertiary else AbbayColors.content
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private const val LARGE_FONT_SCALE = 1.5f
private const val ACTIONS_PER_ROW_WITH_LARGE_TEXT = 3
