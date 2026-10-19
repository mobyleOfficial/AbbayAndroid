package com.mobyle.abbay.presentation.effects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.effects.AudioEffects
import com.effects.BoostLevel
import com.effects.NoiseReductionLevel
import com.effects.SilenceTrimLevel
import com.mobyle.abbay.R
import kotlin.math.roundToInt

/**
 * Audio sheet: silence trim, boost, equalizer, mono/balance and pitch. Noise reduction joins
 * with FX-07. Every change is saved for the book and heard at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(panel: AudioEffectsPanel, onDismiss: () -> Unit) {
    val effects = panel.effects ?: AudioEffects()
    val change = panel.onChange
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.effects_title), style = MaterialTheme.typography.titleMedium)
            if (panel.isCasting) Text(stringResource(R.string.effects_not_on_cast), style = MaterialTheme.typography.bodyMedium)
            NoiseSection(effects) { level -> change(effects.copy(noiseReduction = level)) }
            SilenceSection(effects) { level -> change(effects.copy(silenceTrim = level)) }
            BoostSection(effects) { level -> change(effects.copy(boost = level)) }
            EqualizerSection(effects, panel)
            ChannelsSection(effects, change)
            PitchSection(effects) { semitones -> change(effects.copy(pitchSemitones = semitones)) }
        }
    }
}

/** First in the sheet, as it is first in the processing chain. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoiseSection(effects: AudioEffects, onSelect: (NoiseReductionLevel) -> Unit) {
    SectionTitle(R.string.effects_noise_reduction)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoiseReductionLevel.entries.forEach { level ->
            FilterChip(selected = effects.noiseReduction == level, onClick = {
                onSelect(level)
            }, label = { Text(stringResource(level.labelResource())) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SilenceSection(effects: AudioEffects, onSelect: (SilenceTrimLevel) -> Unit) {
    SectionTitle(R.string.effects_silence_trim)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SilenceTrimLevel.entries.forEach { level ->
            FilterChip(selected = effects.silenceTrim == level, onClick = {
                onSelect(level)
            }, label = { Text(stringResource(level.labelResource())) })
        }
    }
    if (effects.silenceSavedMs >= MILLISECONDS_PER_MINUTE) {
        Text(savedLabel(effects.silenceSavedMs), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoostSection(effects: AudioEffects, onSelect: (BoostLevel) -> Unit) {
    SectionTitle(R.string.effects_boost)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BoostLevel.entries.forEach { level ->
            FilterChip(selected = effects.boost == level, onClick = {
                onSelect(level)
            }, label = { Text(stringResource(level.labelResource())) })
        }
    }
}

@Composable
private fun ChannelsSection(effects: AudioEffects, change: (AudioEffects) -> Unit) {
    SectionTitle(R.string.effects_channels)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.effects_mono), modifier = Modifier.weight(1f))
        Switch(
            checked = effects.mono,
            onCheckedChange = { change(effects.copy(mono = it)) },
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary),
        )
    }
    var balance by remember(effects.balance) { mutableFloatStateOf(effects.balance) }
    val balanceLabel = stringResource(R.string.effects_balance)
    val balanceState = balanceDescription(balance)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.effects_balance_left))
        Slider(
            value = balance,
            onValueChange = { balance = (it * BALANCE_STEPS).roundToInt() / BALANCE_STEPS },
            onValueChangeFinished = { change(effects.copy(balance = balance)) },
            valueRange = -1f..1f,
            steps = BALANCE_STEPS.toInt() * 2 - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics {
                    contentDescription = balanceLabel
                    stateDescription = balanceState
                },
        )
        Text(stringResource(R.string.effects_balance_right))
    }
}

@Composable
private fun PitchSection(effects: AudioEffects, onChange: (Float) -> Unit) {
    var semitones by remember(effects.pitchSemitones) { mutableFloatStateOf(effects.pitchSemitones) }
    SectionTitle(R.string.effects_pitch)
    val label = pitchLabel(semitones)
    Text(label, style = MaterialTheme.typography.bodyMedium)
    val pitchDescription = stringResource(R.string.effects_pitch)
    Slider(
        value = semitones,
        onValueChange = { semitones = (it / AudioEffects.PITCH_STEP).roundToInt() * AudioEffects.PITCH_STEP },
        onValueChangeFinished = { onChange(semitones) },
        valueRange = -AudioEffects.MAXIMUM_PITCH_SEMITONES..AudioEffects.MAXIMUM_PITCH_SEMITONES,
        steps = (2 * AudioEffects.MAXIMUM_PITCH_SEMITONES / AudioEffects.PITCH_STEP).toInt() - 1,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.tertiary,
            activeTrackColor = MaterialTheme.colorScheme.tertiary
        ),
        modifier = Modifier.semantics {
            contentDescription = pitchDescription
            stateDescription = label
        },
    )
}

@Composable
internal fun SectionTitle(labelResource: Int) {
    Text(
        stringResource(labelResource),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun savedLabel(milliseconds: Long): String {
    val minutes = (milliseconds / MILLISECONDS_PER_MINUTE).toInt()
    return stringResource(R.string.effects_saved_in_book, minutes)
}

@Composable
private fun pitchLabel(semitones: Float): String = when {
    semitones == 0f -> stringResource(R.string.effects_pitch_original)
    else -> stringResource(R.string.effects_pitch_semitones, "%+.1f".format(semitones))
}

@Composable
private fun balanceDescription(balance: Float): String = when {
    balance < 0f -> stringResource(R.string.effects_balance_toward_left, (-balance * PERCENT).roundToInt())
    balance > 0f -> stringResource(R.string.effects_balance_toward_right, (balance * PERCENT).roundToInt())
    else -> stringResource(R.string.effects_balance_center)
}

private const val MILLISECONDS_PER_MINUTE = 60_000L
private const val BALANCE_STEPS = 10f
private const val PERCENT = 100
