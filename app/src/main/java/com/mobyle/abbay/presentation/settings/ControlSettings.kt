package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import com.settings.AppSettings
import com.settings.HeadsetLongPressAction
import com.settings.HeadsetSkipAction
import com.settings.ShortInterruptionBehavior

/** Settings → Controls. */
@Composable
internal fun ControlSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        SectionHeader(R.string.settings_controls)
        ChoiceSetting(
            label = stringResource(R.string.settings_short_interruptions),
            options = ShortInterruptionBehavior.entries,
            selected = settings.shortInterruptions,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { behavior -> change { it.copy(shortInterruptions = behavior) } },
        )
        SwitchSetting(R.string.settings_resume_after_calls, settings.resumeAfterCalls) { enabled ->
            change { it.copy(resumeAfterCalls = enabled) }
        }
        SwitchSetting(R.string.settings_resume_on_headset, settings.resumeOnHeadsetConnect) { enabled ->
            change { it.copy(resumeOnHeadsetConnect = enabled) }
        }
        ChoiceSetting(
            label = stringResource(R.string.settings_headset_next),
            options = HeadsetSkipAction.entries,
            selected = settings.headsetNextAction,
            optionLabel = { stringResource(it.nextLabelResource()) },
            onSelect = { action -> change { it.copy(headsetNextAction = action) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_headset_previous),
            options = HeadsetSkipAction.entries,
            selected = settings.headsetPreviousAction,
            optionLabel = { stringResource(it.previousLabelResource()) },
            onSelect = { action -> change { it.copy(headsetPreviousAction = action) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_headset_long_press),
            options = HeadsetLongPressAction.entries,
            selected = settings.headsetLongPressAction,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { action -> change { it.copy(headsetLongPressAction = action) } },
        )
        SwitchSetting(R.string.settings_sleep_in_car, settings.sleepInCar) { enabled -> change { it.copy(sleepInCar = enabled) } }
        SwitchSetting(R.string.settings_face_down_to_pause, settings.faceDownToPause) { enabled ->
            change { it.copy(faceDownToPause = enabled) }
        }
        SwitchSetting(R.string.settings_flip_to_rewind, settings.flipToRewind) { enabled -> change { it.copy(flipToRewind = enabled) } }
        SwitchSetting(R.string.settings_show_cast_button, settings.showCastButton) { enabled ->
            change { it.copy(showCastButton = enabled) }
        }
        SwitchSetting(R.string.settings_cast_processed_audio, settings.castProcessedAudio) { enabled ->
            change { it.copy(castProcessedAudio = enabled) }
        }
        QuickActionSettings(settings, change)
    }
}

private fun ShortInterruptionBehavior.labelResource(): Int = when (this) {
    ShortInterruptionBehavior.PAUSE -> R.string.interruptions_pause
    ShortInterruptionBehavior.LOWER_VOLUME -> R.string.interruptions_lower_volume
}

private fun HeadsetSkipAction.nextLabelResource(): Int = when (this) {
    HeadsetSkipAction.SKIP -> R.string.headset_skip_forward
    HeadsetSkipAction.CHAPTER -> R.string.headset_next_chapter
    HeadsetSkipAction.TRACK -> R.string.headset_next_track
}

private fun HeadsetSkipAction.previousLabelResource(): Int = when (this) {
    HeadsetSkipAction.SKIP -> R.string.headset_skip_back
    HeadsetSkipAction.CHAPTER -> R.string.headset_previous_chapter
    HeadsetSkipAction.TRACK -> R.string.headset_previous_track
}

private fun HeadsetLongPressAction.labelResource(): Int = when (this) {
    HeadsetLongPressAction.BOOKMARK -> R.string.headset_long_bookmark
    HeadsetLongPressAction.SLEEP_TIMER -> R.string.headset_long_sleep
    HeadsetLongPressAction.NONE -> R.string.headset_long_none
}
