package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import kotlin.math.roundToInt

/** Slider 0.5–3.5 in 0.05 steps, preset chips, ±0.05 buttons, "Set as default for new books". */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeedSheet(speed: Float, onSpeedChange: (Float) -> Unit, onSetDefault: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.speed_sheet_title), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                IconButton(onClick = { onSpeedChange(SpeedSteps.snap(speed - SpeedSteps.STEP)) }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.speed_decrease))
                }
                Text(
                    formatSpeed(speed),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { onSpeedChange(SpeedSteps.snap(speed + SpeedSteps.STEP)) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.speed_increase))
                }
            }
            Slider(
                value = speed,
                onValueChange = { onSpeedChange(SpeedSteps.snap(it)) },
                valueRange = SpeedSteps.MINIMUM..SpeedSteps.MAXIMUM,
                steps = SpeedSteps.SLIDER_STEPS,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                ),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SpeedSteps.PRESETS.forEach { preset ->
                    FilterChip(selected = speed == preset, onClick = { onSpeedChange(preset) }, label = { Text(formatSpeed(preset)) })
                }
            }
            AbbayTextButton(onClick = { onSetDefault(speed) }, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(stringResource(R.string.speed_set_default))
            }
        }
    }
}

/** Speed range and steps. */
object SpeedSteps {
    const val MINIMUM = 0.5f
    const val MAXIMUM = 3.5f
    const val STEP = 0.05f
    val PRESETS = listOf(0.8f, 1.0f, 1.2f, 1.5f, 1.75f, 2.0f, 2.5f)

    /** Values between the ends of the slider: (3.5 − 0.5) / 0.05 − 1. */
    val SLIDER_STEPS = ((MAXIMUM - MINIMUM) / STEP).roundToInt() - 1

    /** Rounds to the nearest 0.05 inside the range, avoiding float drift such as 1.2000001. */
    fun snap(value: Float): Float = ((value.coerceIn(MINIMUM, MAXIMUM) / STEP).roundToInt() * STEP * HUNDRED).roundToInt() / HUNDRED

    private const val HUNDRED = 100f
}
