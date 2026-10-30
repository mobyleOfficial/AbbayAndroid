package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.library.LibraryLayout
import com.library.LibraryTab
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.widgets.labelResource
import com.mobyle.abbay.presentation.booklist.widgets.player.SpeedSteps
import com.mobyle.abbay.presentation.booklist.widgets.player.formatSpeed
import com.settings.AppSettings
import com.settings.NotificationSkipButtons
import com.settings.QuickAction
import com.settings.RescanOnStart

@Composable
internal fun LibraryPreferenceSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        ChoiceSetting(
            label = stringResource(R.string.settings_default_tab),
            options = listOf<LibraryTab?>(null) + settings.visibleLibraryTabs,
            selected = settings.libraryTab,
            optionLabel = { tab -> stringResource(tab?.labelResource() ?: R.string.settings_tab_automatic) },
            onSelect = { tab -> change { it.copy(libraryTab = tab) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_library_layout),
            options = LibraryLayout.entries,
            selected = settings.libraryLayout,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { layout -> change { it.copy(libraryLayout = layout) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_rescan_on_start),
            options = RescanOnStart.entries,
            selected = settings.rescanOnStart,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { rescan -> change { it.copy(rescanOnStart = rescan) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_missing_retention),
            options = RETENTION_DAYS,
            selected = settings.missingBookRetentionDays,
            optionLabel = { days -> pluralStringResource(R.plurals.settings_days, days, days) },
            onSelect = { days -> change { it.copy(missingBookRetentionDays = days) } },
        )
        LibraryTabSettings(settings, change)
    }
}

@Composable
internal fun SkipAndSpeedSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        ChoiceSetting(
            label = stringResource(R.string.settings_default_speed),
            options = (SpeedSteps.PRESETS + settings.defaultSpeed).distinct().sorted(),
            selected = settings.defaultSpeed,
            optionLabel = { formatSpeed(it) },
            onSelect = { speed -> change { it.copy(defaultSpeed = speed) } },
        )
        SecondsSetting(R.string.settings_small_back, SMALL_SKIP_SECONDS, settings.smallBackSeconds) { seconds ->
            change { it.copy(smallBackSeconds = seconds) }
        }
        SecondsSetting(R.string.settings_small_forward, SMALL_SKIP_SECONDS, settings.smallForwardSeconds) { seconds ->
            change { it.copy(smallForwardSeconds = seconds) }
        }
        MinutesSetting(R.string.settings_large_back, LARGE_SKIP_MINUTES, settings.largeBackSeconds / SECONDS_PER_MINUTE) { minutes ->
            change { it.copy(largeBackSeconds = minutes * SECONDS_PER_MINUTE) }
        }
        MinutesSetting(R.string.settings_large_forward, LARGE_SKIP_MINUTES, settings.largeForwardSeconds / SECONDS_PER_MINUTE) { minutes ->
            change { it.copy(largeForwardSeconds = minutes * SECONDS_PER_MINUTE) }
        }
        ChoiceSetting(
            label = stringResource(R.string.settings_auto_stop),
            options = AUTO_STOP_MINUTES,
            selected = settings.autoStopAfterPauseMinutes,
            optionLabel = { minutes ->
                if (minutes == 0) stringResource(R.string.settings_never) else stringResource(R.string.sleep_minutes, minutes)
            },
            onSelect = { minutes -> change { it.copy(autoStopAfterPauseMinutes = minutes) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.settings_notification_buttons),
            options = NotificationSkipButtons.entries,
            selected = settings.skipButtonsInNotification,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { buttons -> change { it.copy(skipButtonsInNotification = buttons) } },
        )
    }
}

@Composable
internal fun QuickActionSettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    Column {
        SectionHeader(R.string.settings_quick_actions)
        QuickAction.entries.forEach { action ->
            SwitchSetting(action.labelResource(), action !in settings.hiddenQuickActions) { isShown ->
                change { it.copy(hiddenQuickActions = if (isShown) it.hiddenQuickActions - action else it.hiddenQuickActions + action) }
            }
        }
    }
}

@Composable
private fun SecondsSetting(labelResource: Int, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    ChoiceSetting(
        label = stringResource(labelResource),
        options = (options + selected).distinct().sorted(),
        selected = selected,
        optionLabel = { stringResource(R.string.settings_seconds, it) },
        onSelect = onSelect,
    )
}

@Composable
private fun MinutesSetting(labelResource: Int, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    ChoiceSetting(
        label = stringResource(labelResource),
        options = (options + selected).distinct().sorted(),
        selected = selected,
        optionLabel = { stringResource(R.string.sleep_minutes, it) },
        onSelect = onSelect,
    )
}

private fun RescanOnStart.labelResource(): Int = when (this) {
    RescanOnStart.OFF -> R.string.rescan_off
    RescanOnStart.QUICK -> R.string.rescan_quick
    RescanOnStart.FULL -> R.string.rescan_full
}

private fun NotificationSkipButtons.labelResource(): Int = when (this) {
    NotificationSkipButtons.TIME_SKIP -> R.string.notification_buttons_time
    NotificationSkipButtons.PREVIOUS_AND_NEXT_CHAPTER -> R.string.notification_buttons_chapters
}

private fun QuickAction.labelResource(): Int = when (this) {
    QuickAction.SLEEP_TIMER -> R.string.player_sleep_timer
    QuickAction.BOOKMARK -> R.string.player_add_bookmark
    QuickAction.AUDIO_EFFECTS -> R.string.player_audio_effects
    QuickAction.REPEAT -> R.string.repeat_title
    QuickAction.SKIP_INTRO_OUTRO -> R.string.skip_title
}

private val RETENTION_DAYS = listOf(7, 30, 90, 365)
private val SMALL_SKIP_SECONDS = listOf(5, 10, 15, 20, 30, 45, 60)
private val LARGE_SKIP_MINUTES = listOf(1, 2, 5, 10, 15)
private val AUTO_STOP_MINUTES = listOf(10, 15, 30, 60, 0)
private const val SECONDS_PER_MINUTE = 60
