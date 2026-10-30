package com.mobyle.abbay.presentation.effects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.effects.AudioEffects
import com.effects.EqualizerPreset
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import kotlin.math.roundToInt

/** Equalizer of the audio sheet: presets, 10 vertical sliders, save and apply to all. */
@Composable
internal fun EqualizerSection(effects: AudioEffects, panel: AudioEffectsPanel) {
    var gains by remember(effects.equalizerGains) { mutableStateOf(effects.equalizerGains) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val save: (List<Int>) -> Unit = { panel.onChange(effects.copy(equalizerGains = it)) }

    if (showSaveDialog) {
        PresetNameDialog(
            onSave = { name ->
                panel.onSavePreset(name, gains)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    SectionTitle(R.string.effects_equalizer)
    PresetMenu(gains, panel.presets, onSelect = { preset ->
        gains = preset.gains
        save(preset.gains)
    }, onDelete = panel.onDeletePreset)
    Row(modifier = Modifier.fillMaxWidth().height(SLIDER_LENGTH), horizontalArrangement = Arrangement.SpaceBetween) {
        BAND_LABELS.forEachIndexed { band, label ->
            BandSlider(
                label = label,
                gainDb = gains[band],
                onChange = { value -> gains = gains.toMutableList().also { it[band] = value } },
                onChangeFinished = { save(gains) },
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { showSaveDialog = true }) { Text(stringResource(R.string.effects_save_preset)) }
        OutlinedButton(onClick = { panel.onApplyEqualizerToAll(gains) }) { Text(stringResource(R.string.effects_apply_to_all)) }
    }
}

@Composable
private fun PresetMenu(gains: List<Int>, presets: List<EqualizerPreset>, onSelect: (EqualizerPreset) -> Unit, onDelete: (Long) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    val current = presets.firstOrNull { it.gains == gains }
    Box {
        AbbayTextButton(onClick = { isOpen = true }) {
            Text(current?.let { presetName(it) } ?: stringResource(R.string.effects_preset_custom))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        AbbayDropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            presets.forEach { preset ->
                val name = presetName(preset)
                AbbayMenuItem(
                    text = name,
                    selected = preset == current,
                    // A user preset's delete button takes the trailing place of the check mark.
                    leadingIcon = if (preset == current && !preset.isBuiltIn) Icons.Default.Check else null,
                    trailingIcon = if (preset.isBuiltIn) {
                        null
                    } else {
                        { tint ->
                            IconButton(onClick = { onDelete(preset.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.effects_delete_preset, name),
                                    tint = tint
                                )
                            }
                        }
                    },
                    onClick = {
                        isOpen = false
                        onSelect(preset)
                    },
                )
            }
        }
    }
}

@Composable
private fun BandSlider(label: String, gainDb: Int, onChange: (Int) -> Unit, onChangeFinished: () -> Unit) {
    val bandDescription = stringResource(R.string.effects_band, label)
    val gainText = stringResource(R.string.effects_decibels, "%+d".format(gainDb))
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(BAND_WIDTH)) {
        Text("%+d".format(gainDb), style = MaterialTheme.typography.labelSmall)
        Slider(
            value = gainDb.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            onValueChangeFinished = onChangeFinished,
            valueRange = -AudioEffects.MAXIMUM_BAND_DB.toFloat()..AudioEffects.MAXIMUM_BAND_DB.toFloat(),
            steps = AudioEffects.MAXIMUM_BAND_DB * 2 - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier
                .weight(1f)
                .vertical()
                .semantics {
                    contentDescription = bandDescription
                    stateDescription = gainText
                },
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** A horizontal slider turned upright: the layout swaps width and height, then the drawing is rotated. */
private fun Modifier.vertical(): Modifier = graphicsLayer { rotationZ = UPRIGHT_DEGREES }.layout { measurable, constraints ->
    val placeable = measurable.measure(
        Constraints(minWidth = constraints.minHeight, maxWidth = constraints.maxHeight, minHeight = 0, maxHeight = constraints.maxWidth),
    )
    layout(placeable.height, placeable.width) {
        placeable.place(-(placeable.width - placeable.height) / 2, (placeable.width - placeable.height) / 2)
    }
}

@Composable
private fun PresetNameDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AbbayAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.effects_save_preset)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAXIMUM_NAME_LENGTH) },
                label = { Text(stringResource(R.string.effects_preset_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            AbbayTextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { AbbayTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun presetName(preset: EqualizerPreset): String = when (preset.id) {
    EqualizerPreset.FLAT.id -> stringResource(R.string.effects_preset_flat)
    EqualizerPreset.VOICE_CLARITY.id -> stringResource(R.string.effects_preset_voice_clarity)
    EqualizerPreset.REDUCE_HISS.id -> stringResource(R.string.effects_preset_reduce_hiss)
    EqualizerPreset.WARM.id -> stringResource(R.string.effects_preset_warm)
    EqualizerPreset.SMALL_SPEAKER.id -> stringResource(R.string.effects_preset_small_speaker)
    else -> preset.name
}

private val BAND_LABELS = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
private val SLIDER_LENGTH = 200.dp
private val BAND_WIDTH = 30.dp
private const val UPRIGHT_DEGREES = 270f
private const val MAXIMUM_NAME_LENGTH = 40
