package com.mobyle.abbay.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.effects.BoostLevel
import com.effects.EqualizerPreset
import com.effects.NoiseReductionLevel
import com.effects.SilenceTrimLevel
import com.listening.ShakeSensitivity
import com.listening.SleepTimer
import com.listening.SmartRewindLevel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.player.labelResource
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.effects.labelResource
import com.mobyle.abbay.presentation.effects.presetName
import com.settings.AppSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Playback and Sleep timer sections of Settings. */
@Composable
fun ListeningSettings(viewModel: SettingsViewModel) {
    val settings by viewModel.listeningSettings.collectAsState()
    val change = viewModel::changeListeningSettings

    Column {
        SectionHeader(R.string.settings_playback)
        ChoiceSetting(
            label = stringResource(R.string.settings_smart_rewind),
            options = SmartRewindLevel.entries,
            selected = settings.smartRewind,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { level -> change { it.copy(smartRewind = level) } },
        )
        SkipAndSpeedSettings(settings, change)
        SwitchSetting(R.string.settings_continue_with_queue, settings.continueWithQueue) { enabled ->
            change { it.copy(continueWithQueue = enabled) }
        }
        StatisticsSettings(settings.statisticsEnabled, change, onClear = viewModel::clearListeningStatistics)

        AudioDefaultSettings(settings, change)
        SleepTimerSettings(settings, change)
        ControlSettings(settings, change)
    }
}

/** Recording on/off and "Clear statistics" with a confirmation. */
@Composable
private fun StatisticsSettings(isEnabled: Boolean, change: ((AppSettings) -> AppSettings) -> Unit, onClear: () -> Unit) {
    var isConfirming by remember { mutableStateOf(false) }
    val context = LocalContext.current
    SwitchSetting(R.string.settings_statistics, isEnabled) { enabled -> change { it.copy(statisticsEnabled = enabled) } }
    SettingItem(text = stringResource(R.string.settings_clear_statistics), onClick = { isConfirming = true })
    if (isConfirming) {
        AbbayAlertDialog(
            onDismissRequest = { isConfirming = false },
            title = { Text(stringResource(R.string.settings_clear_statistics)) },
            text = { Text(stringResource(R.string.settings_clear_statistics_body)) },
            confirmButton = {
                AbbayTextButton(onClick = {
                    isConfirming = false
                    onClear()
                    Toast.makeText(context, R.string.settings_statistics_cleared, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { AbbayTextButton(onClick = { isConfirming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Effects of books never played yet (Audio); built-in equalizer presets only. */
@Composable
private fun AudioDefaultSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        SectionHeader(R.string.settings_audio)
        ChoiceSetting(
            label = stringResource(R.string.settings_default_silence_trim),
            options = SilenceTrimLevel.entries,
            selected = settings.defaultSilenceTrim,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { level -> change { it.copy(defaultSilenceTrim = level) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_default_boost),
            options = BoostLevel.entries,
            selected = settings.defaultBoost,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { level -> change { it.copy(defaultBoost = level) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_default_equalizer),
            options = EqualizerPreset.BUILT_IN,
            selected = EqualizerPreset.BUILT_IN.firstOrNull { it.id == settings.defaultEqualizerPresetId } ?: EqualizerPreset.FLAT,
            optionLabel = { presetName(it) },
            onSelect = { preset -> change { it.copy(defaultEqualizerPresetId = preset.id) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_default_noise_reduction),
            options = NoiseReductionLevel.entries,
            selected = settings.defaultNoiseReduction,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { level -> change { it.copy(defaultNoiseReduction = level) } },
        )
    }
}

@Composable
private fun SleepTimerSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        SectionHeader(R.string.settings_sleep_timer)
        ChoiceSetting(
            label = stringResource(R.string.settings_sleep_default),
            options = SleepTimer.PRESET_MINUTES,
            selected = settings.sleepDefaultMinutes,
            optionLabel = { stringResource(R.string.sleep_minutes, it) },
            onSelect = { minutes -> change { it.copy(sleepDefaultMinutes = minutes) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_sleep_fade),
            options = FADE_SECONDS,
            selected = settings.sleepFadeSeconds,
            optionLabel = { stringResource(R.string.settings_seconds, it) },
            onSelect = { seconds -> change { it.copy(sleepFadeSeconds = seconds) } },
        )
        SwitchSetting(R.string.settings_shake_to_extend, settings.shakeToExtend) { enabled -> change { it.copy(shakeToExtend = enabled) } }
        if (settings.shakeToExtend) {
            ChoiceSetting(
                label = stringResource(R.string.shake_sensitivity),
                options = ShakeSensitivity.entries,
                selected = settings.shakeSensitivity,
                optionLabel = { stringResource(it.labelResource()) },
                onSelect = { sensitivity -> change { it.copy(shakeSensitivity = sensitivity) } },
            )
            SwitchSetting(R.string.settings_extend_only_during_fade, settings.extendOnlyDuringFade) { enabled ->
                change { it.copy(extendOnlyDuringFade = enabled) }
            }
        }
        ChimeAndScheduleSettings(settings, change)
    }
}

@Composable
private fun ChimeAndScheduleSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        SwitchSetting(R.string.settings_sleep_chime, settings.sleepChime) { enabled -> change { it.copy(sleepChime = enabled) } }
        if (settings.sleepChime) {
            ChoiceSetting(
                label = stringResource(R.string.settings_sleep_chime_volume),
                options = CHIME_VOLUMES,
                selected = settings.sleepChimeVolumePercent,
                optionLabel = { stringResource(R.string.player_percent, it) },
                onSelect = { volume -> change { it.copy(sleepChimeVolumePercent = volume) } },
            )
        }
        SwitchSetting(R.string.settings_auto_sleep, settings.autoSleepEnabled) { enabled -> change { it.copy(autoSleepEnabled = enabled) } }
        if (settings.autoSleepEnabled) {
            TimeSetting(R.string.settings_auto_sleep_from, settings.autoSleepStartMinute) { minute ->
                change { it.copy(autoSleepStartMinute = minute) }
            }
            TimeSetting(R.string.settings_auto_sleep_until, settings.autoSleepEndMinute) { minute ->
                change { it.copy(autoSleepEndMinute = minute) }
            }
        }
    }
}

@Composable
internal fun SwitchSetting(labelResource: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingItem(text = stringResource(labelResource)) { SettingSwitch(checked, onChange) }
}

@Composable
internal fun <T> ChoiceSetting(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        SettingItem(text = label, onClick = { isMenuOpen = true }) { SettingValue(optionLabel(selected)) }
        AbbayDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            options.forEach { option ->
                AbbayMenuItem(
                    text = optionLabel(option),
                    selected = option == selected,
                    onClick = {
                        isMenuOpen = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSetting(labelResource: Int, minuteOfDay: Int, onChange: (Int) -> Unit) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val time = LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    SettingItem(text = stringResource(labelResource), onClick = { isPickerOpen = true }) {
        SettingValue(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
    }
    if (isPickerOpen) {
        val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = is24Hour)
        AbbayAlertDialog(
            onDismissRequest = { isPickerOpen = false },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                AbbayTextButton(onClick = {
                    isPickerOpen = false
                    onChange(pickerState.hour * MINUTES_PER_HOUR + pickerState.minute)
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { AbbayTextButton(onClick = { isPickerOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SettingValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(end = 16.dp),
    )
}

private fun SmartRewindLevel.labelResource(): Int = when (this) {
    SmartRewindLevel.OFF -> R.string.smart_rewind_off
    SmartRewindLevel.LIGHT -> R.string.smart_rewind_light
    SmartRewindLevel.STANDARD -> R.string.smart_rewind_standard
    SmartRewindLevel.GENEROUS -> R.string.smart_rewind_generous
}

private val FADE_SECONDS = listOf(0, 10, 20, 30, 45, 60, 90, 120)
private val CHIME_VOLUMES = listOf(25, 50, 75, 100)
private const val MINUTES_PER_HOUR = 60
