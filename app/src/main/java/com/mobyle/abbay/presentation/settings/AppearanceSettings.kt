package com.mobyle.abbay.presentation.settings

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.infra.ads.LocalAdsSession
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayDropdownMenu
import com.mobyle.abbay.presentation.common.widgets.AbbayMenuItem
import com.settings.AppLanguage
import com.settings.AppSettings
import com.settings.ProgressLabels
import com.settings.ThemeMode

/** Appearance. */
@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val settings by viewModel.listeningSettings.collectAsState()
    val language by viewModel.language.collectAsState()
    val activity = LocalContext.current as? Activity
    var isRecreationPending by remember { mutableStateOf(false) }

    Column {
        SectionHeader(R.string.settings_appearance)
        ThemeSetting(themeMode, viewModel::changeThemeMode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingItem(text = stringResource(R.string.settings_dynamic_color)) {
                SettingSwitch(dynamicColor, viewModel::changeDynamicColor)
            }
        }
        DisplaySettings(settings, viewModel::changeListeningSettings)
        ChoiceSetting(
            label = stringResource(R.string.settings_language),
            options = AppLanguage.entries,
            selected = language,
            optionLabel = { stringResource(it.labelResource()) },
            onSelect = { chosen ->
                if (chosen != language) {
                    viewModel.changeLanguage(chosen)
                    // Android 13+ recreates the activity itself; before, the app applies the language.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) isRecreationPending = true
                }
            },
        )
        if (isRecreationPending) {
            // After the menu's popup has left the composition: recreating while its window closes left the activity
            // without a focused window (ANR on the Android 12 emulator).
            LaunchedEffect(Unit) {
                withFrameNanos { }
                activity?.recreate()
            }
        }
    }
}

/** "Send crash reports and usage statistics", "Show ads" and what is sent. */
@Composable
fun PrivacySettings(viewModel: SettingsViewModel) {
    val shareDiagnostics by viewModel.shareDiagnostics.collectAsState()
    Column {
        SettingItem(text = stringResource(R.string.settings_share_diagnostics)) {
            SettingSwitch(shareDiagnostics, viewModel::changeShareDiagnostics)
        }
        AdsSettings(viewModel)
        Text(
            text = stringResource(R.string.settings_privacy_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** "Show ads", read at startup, and the ad consent choices where the law requires them. */
@Composable
private fun AdsSettings(viewModel: SettingsViewModel) {
    val showAds by viewModel.showAds.collectAsState()
    val showDisableAdsConfirmation by viewModel.showDisableAdsConfirmation.collectAsState()
    val adsSession = LocalAdsSession.current
    val isPrivacyOptionsRequired = adsSession?.isPrivacyOptionsRequired?.collectAsState()?.value == true
    val activity = LocalContext.current as? Activity

    SettingItem(text = stringResource(R.string.settings_show_ads)) {
        SettingSwitch(showAds, viewModel::changeShowAds)
    }
    Text(
        text = stringResource(R.string.settings_show_ads_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    if (isPrivacyOptionsRequired && activity != null) {
        SettingItem(text = stringResource(R.string.settings_ad_privacy_options), onClick = { adsSession?.showPrivacyOptions(activity) })
    }

    if (showDisableAdsConfirmation) {
        AbbayActionDialog(
            onDismiss = viewModel::dismissDisableAdsConfirmation,
            title = stringResource(R.string.disable_ads_dialog_title),
            body = stringResource(R.string.disable_ads_dialog_body),
            actionButtonTitle = stringResource(R.string.disable_ads_dialog_action),
            onAction = viewModel::confirmDisableAds,
            icon = Icons.Default.Info,
            isDestructive = false,
        )
    }
}

@Composable
private fun DisplaySettings(settings: AppSettings, change: ((AppSettings) -> AppSettings) -> Unit) {
    SwitchSetting(R.string.settings_cover_on_lock_screen, settings.coverOnLockScreen) { enabled ->
        change { it.copy(coverOnLockScreen = enabled) }
    }
    SwitchSetting(R.string.settings_keep_screen_on, settings.keepScreenOnWhileCharging) { enabled ->
        change { it.copy(keepScreenOnWhileCharging = enabled) }
    }
    ChoiceSetting(
        label = stringResource(R.string.settings_progress_labels),
        options = ProgressLabels.entries,
        selected = settings.progressLabels,
        optionLabel = { labels ->
            stringResource(
                when (labels) {
                    ProgressLabels.ELAPSED_AND_REMAINING -> R.string.progress_elapsed_remaining
                    ProgressLabels.ELAPSED_AND_TOTAL -> R.string.progress_elapsed_total
                }
            )
        },
        onSelect = { labels -> change { it.copy(progressLabels = labels) } },
    )
}

@Composable
private fun ThemeSetting(themeMode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        SettingItem(text = stringResource(R.string.settings_theme), onClick = { isMenuOpen = true }) {
            Text(
                text = stringResource(themeMode.labelResource()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        AbbayDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            ThemeMode.entries.forEach { mode ->
                AbbayMenuItem(
                    text = stringResource(mode.labelResource()),
                    selected = mode == themeMode,
                    onClick = {
                        isMenuOpen = false
                        onChange(mode)
                    },
                )
            }
        }
    }
}

@Composable
internal fun SettingSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.tertiary,
            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

@Composable
internal fun SectionHeader(labelResource: Int) {
    Text(
        text = stringResource(labelResource),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

private fun AppLanguage.labelResource(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.PORTUGUESE -> R.string.language_portuguese
    AppLanguage.SPANISH -> R.string.language_spanish
}

private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.BLACK -> R.string.theme_black
}
