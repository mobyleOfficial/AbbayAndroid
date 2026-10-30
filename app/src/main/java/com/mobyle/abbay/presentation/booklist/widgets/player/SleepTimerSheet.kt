package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.listening.ShakeSensitivity
import com.listening.SleepTimer
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.common.widgets.abbayTextFieldColors
import com.player.SleepTimerMode
import com.player.SleepTimerState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    settings: SleepSheetSettings,
    onStart: (SleepTimerMode) -> Unit,
    onCancel: () -> Unit,
    onShakeSensitivityChange: (ShakeSensitivity) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    if (showCustomDialog) {
        CustomDurationDialog(
            initialMinutes = settings.defaultMinutes,
            onConfirm = { minutes ->
                showCustomDialog = false
                onStart(SleepTimerMode.Duration(minutes))
                onDismiss()
            },
            onDismiss = { showCustomDialog = false },
        )
    }

    val start: (SleepTimerMode) -> Unit = { mode ->
        onStart(mode)
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.sleep_sheet_title), style = MaterialTheme.typography.titleMedium)
            if (state is SleepTimerState.Running) {
                RunningTimer(state) {
                    onCancel()
                    onDismiss()
                }
            }
            ModeChips(onStart = start, onCustom = { showCustomDialog = true })
            if (settings.isShakeEnabled) {
                Text(
                    stringResource(R.string.shake_sensitivity),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ShakeSensitivity.entries.forEach { sensitivity ->
                        FilterChip(
                            selected = sensitivity == settings.shakeSensitivity,
                            onClick = { onShakeSensitivityChange(sensitivity) },
                            label = { Text(stringResource(sensitivity.labelResource())) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.sleep_shake_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeChips(onStart: (SleepTimerMode) -> Unit, onCustom: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SleepTimer.PRESET_MINUTES.forEach { minutes ->
            AssistChip(onClick = {
                onStart(SleepTimerMode.Duration(minutes))
            }, label = { Text(stringResource(R.string.sleep_minutes, minutes)) })
        }
        AssistChip(onClick = { onStart(SleepTimerMode.EndOfChapter) }, label = { Text(stringResource(R.string.sleep_end_of_chapter)) })
        AssistChip(onClick = { onStart(SleepTimerMode.EndOfTrack) }, label = { Text(stringResource(R.string.sleep_end_of_track)) })
        AssistChip(onClick = onCustom, label = { Text(stringResource(R.string.sleep_custom)) })
    }
}

@Composable
private fun RunningTimer(state: SleepTimerState.Running, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = runningDescription(state),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (state.isFading) Text(stringResource(R.string.sleep_fading), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.sleep_turn_off)) }
    }
}

@Composable
fun runningDescription(state: SleepTimerState.Running): String {
    val remaining = formatCountdown(state.remainingMs)
    return when (state.mode) {
        is SleepTimerMode.Duration -> stringResource(R.string.sleep_remaining, remaining)
        SleepTimerMode.EndOfChapter -> stringResource(R.string.sleep_running_end_of_chapter, remaining)
        SleepTimerMode.EndOfTrack -> stringResource(R.string.sleep_running_end_of_track, remaining)
    }
}

@Composable
private fun CustomDurationDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialMinutes.toString()) }
    val minutes = text.toIntOrNull()?.takeIf { it in SleepTimer.MINIMUM_CUSTOM_MINUTES..SleepTimer.MAXIMUM_CUSTOM_MINUTES }
    AbbayAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_custom_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(MAXIMUM_DIGITS) },
                label = { Text(stringResource(R.string.sleep_custom_label)) },
                isError = minutes == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = abbayTextFieldColors(),
            )
        },
        confirmButton = {
            AbbayTextButton(onClick = { minutes?.let(onConfirm) }, enabled = minutes != null) { Text(stringResource(R.string.sleep_start)) }
        },
        dismissButton = { AbbayTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

data class SleepSheetSettings(
    val defaultMinutes: Int = 15,
    val isShakeEnabled: Boolean = true,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
)

fun ShakeSensitivity.labelResource(): Int = when (this) {
    ShakeSensitivity.LOW -> R.string.shake_low
    ShakeSensitivity.MEDIUM -> R.string.shake_medium
    ShakeSensitivity.HIGH -> R.string.shake_high
}

fun formatCountdown(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) + MILLISECONDS_PER_SECOND - 1) / MILLISECONDS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private const val MAXIMUM_DIGITS = 3
private const val MILLISECONDS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
