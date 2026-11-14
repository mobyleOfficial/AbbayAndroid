package com.mobyle.abbay.presentation.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.player.PlaybackOptions
import com.player.RepeatMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepeatSheet(panel: PlaybackOptionsPanel, onDismiss: () -> Unit) {
    val options = panel.options
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.repeat_title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                RepeatMode.entries.forEach { mode ->
                    FilterChip(
                        selected = options.repeatMode == mode,
                        onClick = { panel.onChange(options.copy(repeatMode = mode)) },
                        label = { Text(stringResource(mode.labelResource())) },
                    )
                }
            }
            if (options.repeatMode == RepeatMode.AB) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                    OutlinedButton(onClick = { panel.onSetLoopPoint(true) }) {
                        Text(stringResource(R.string.repeat_set_a, options.loopAMs?.toHHMMSS() ?: "—"))
                    }
                    OutlinedButton(onClick = { panel.onSetLoopPoint(false) }) {
                        Text(stringResource(R.string.repeat_set_b, options.loopBMs?.toHHMMSS() ?: "—"))
                    }
                }
                if (!options.hasLoop) Text(stringResource(R.string.repeat_ab_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipSheet(panel: PlaybackOptionsPanel, onDismiss: () -> Unit) {
    val options = panel.options
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.skip_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.skip_explanation),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            SecondsSlider(R.string.skip_intro, options.skipIntroSeconds) { panel.onChange(options.copy(skipIntroSeconds = it)) }
            SecondsSlider(R.string.skip_outro, options.skipOutroSeconds) { panel.onChange(options.copy(skipOutroSeconds = it)) }
        }
    }
}

@Composable
private fun SecondsSlider(labelResource: Int, seconds: Int, onChange: (Int) -> Unit) {
    var value by remember(seconds) { mutableFloatStateOf(seconds.toFloat()) }
    val label = stringResource(labelResource)
    val valueText = stringResource(R.string.skip_seconds, value.roundToInt())
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("$label · $valueText", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = { value = (it / STEP_SECONDS).roundToInt() * STEP_SECONDS.toFloat() },
            onValueChangeFinished = { onChange(value.roundToInt()) },
            valueRange = 0f..PlaybackOptions.MAXIMUM_SKIP_SECONDS.toFloat(),
            steps = PlaybackOptions.MAXIMUM_SKIP_SECONDS / STEP_SECONDS - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = valueText
            },
        )
    }
}

fun RepeatMode.labelResource(): Int = when (this) {
    RepeatMode.OFF -> R.string.repeat_off
    RepeatMode.CHAPTER -> R.string.repeat_chapter
    RepeatMode.TRACK -> R.string.repeat_track
    RepeatMode.BOOK -> R.string.repeat_book
    RepeatMode.AB -> R.string.repeat_ab
}

private const val STEP_SECONDS = 5
