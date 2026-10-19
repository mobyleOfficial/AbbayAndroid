package com.mobyle.abbay.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.library.LibraryTab
import com.settings.AppSettings
import com.settings.QuickAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@Suppress("TooManyFunctions") // Settings plus the per-book subtitle charset and the legacy values.
class SettingsDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {
    fun observeSettings(): Flow<AppSettings> = dataStore.data.map(::toAppSettings)

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val updated = transform(toAppSettings(preferences))
            preferences[SettingsKeys.playWhenAppIsClosed] = updated.playWhenAppIsClosed
            preferences[SettingsKeys.openPlayerOnStartup] = updated.openPlayerOnStartup
            preferences[SettingsKeys.reloadGuideShown] = updated.reloadGuideShown
            updated.lastBookId?.let { preferences[SettingsKeys.lastBookId] = it } ?: preferences.remove(SettingsKeys.lastBookId)
            preferences[SettingsKeys.librarySortOrder] = updated.librarySortOrder.name
            preferences[SettingsKeys.librarySortAscending] = updated.librarySortAscending
            updated.libraryTab?.let { preferences[SettingsKeys.libraryTab] = it.name } ?: preferences.remove(SettingsKeys.libraryTab)
            preferences[SettingsKeys.libraryLayout] = updated.libraryLayout.name
            preferences[SettingsKeys.missingBookRetentionDays] = updated.missingBookRetentionDays
            preferences[SettingsKeys.smallBackSeconds] = updated.smallBackSeconds
            preferences[SettingsKeys.smallForwardSeconds] = updated.smallForwardSeconds
            preferences[SettingsKeys.largeBackSeconds] = updated.largeBackSeconds
            preferences[SettingsKeys.largeForwardSeconds] = updated.largeForwardSeconds
            preferences[SettingsKeys.defaultSpeed] = updated.defaultSpeed
            preferences[SettingsKeys.themeMode] = updated.themeMode.name
            preferences[SettingsKeys.dynamicColor] = updated.dynamicColor
            preferences[SettingsKeys.shareDiagnostics] = updated.shareDiagnostics
            preferences[SettingsKeys.showAds] = updated.showAds
            updated.pendingCoverBookId?.let { preferences[SettingsKeys.pendingCoverBookId] = it }
                ?: preferences.remove(SettingsKeys.pendingCoverBookId)
            preferences[SettingsKeys.autoStopAfterPauseMinutes] = updated.autoStopAfterPauseMinutes
            preferences[SettingsKeys.smartRewind] = updated.smartRewind.name
            preferences[SettingsKeys.sleepDefaultMinutes] = updated.sleepDefaultMinutes
            preferences[SettingsKeys.sleepFadeSeconds] = updated.sleepFadeSeconds
            preferences[SettingsKeys.shakeToExtend] = updated.shakeToExtend
            preferences[SettingsKeys.shakeSensitivity] = updated.shakeSensitivity.name
            preferences[SettingsKeys.extendOnlyDuringFade] = updated.extendOnlyDuringFade
            preferences[SettingsKeys.sleepChime] = updated.sleepChime
            preferences[SettingsKeys.sleepChimeVolumePercent] = updated.sleepChimeVolumePercent
            preferences[SettingsKeys.autoSleepEnabled] = updated.autoSleepEnabled
            preferences[SettingsKeys.autoSleepStartMinute] = updated.autoSleepStartMinute
            preferences[SettingsKeys.autoSleepEndMinute] = updated.autoSleepEndMinute
            preferences[SettingsKeys.saveProgressInFolders] = updated.saveProgressInFolders
            preferences[SettingsKeys.defaultSilenceTrim] = updated.defaultSilenceTrim.name
            preferences[SettingsKeys.defaultBoost] = updated.defaultBoost.name
            preferences[SettingsKeys.defaultEqualizerPresetId] = updated.defaultEqualizerPresetId
            preferences[SettingsKeys.shortInterruptions] = updated.shortInterruptions.name
            preferences[SettingsKeys.resumeAfterCalls] = updated.resumeAfterCalls
            preferences[SettingsKeys.resumeOnHeadsetConnect] = updated.resumeOnHeadsetConnect
            preferences[SettingsKeys.headsetNextAction] = updated.headsetNextAction.name
            preferences[SettingsKeys.headsetPreviousAction] = updated.headsetPreviousAction.name
            preferences[SettingsKeys.headsetLongPressAction] = updated.headsetLongPressAction.name
            preferences[SettingsKeys.sleepInCar] = updated.sleepInCar
            preferences[SettingsKeys.statisticsEnabled] = updated.statisticsEnabled
            preferences[SettingsKeys.continueWithQueue] = updated.continueWithQueue
            preferences[SettingsKeys.faceDownToPause] = updated.faceDownToPause
            preferences[SettingsKeys.flipToRewind] = updated.flipToRewind
            preferences[SettingsKeys.converterCopyCover] = updated.converterCopyCover
            preferences[SettingsKeys.converterMinimumBatteryPercent] = updated.converterMinimumBatteryPercent
            preferences[SettingsKeys.converterSavedBytes] = updated.converterSavedBytes
            preferences[SettingsKeys.showCastButton] = updated.showCastButton
            writeDisplaySettings(preferences, updated)
        }
    }

    /** The charset chosen for a book's subtitle files, or null. */
    suspend fun subtitleCharset(bookId: Long): String? = dataStore.data.first()[SettingsKeys.subtitleCharset(bookId)]

    suspend fun setSubtitleCharset(bookId: Long, charset: String?) {
        dataStore.edit { preferences ->
            charset?.let { preferences[SettingsKeys.subtitleCharset(bookId)] = it }
                ?: preferences.remove(SettingsKeys.subtitleCharset(bookId))
        }
    }

    suspend fun legacyFolderUri(): String? = dataStore.data.first().let { preferences ->
        if (preferences[SettingsKeys.legacyFolderMigrated] == true) null else preferences[SettingsKeys.legacyFolderUri]
    }

    suspend fun markLegacyFolderMigrated() {
        dataStore.edit { it[SettingsKeys.legacyFolderMigrated] = true }
    }

    suspend fun legacySelectedMediaId(): String? = dataStore.data.first()[SettingsKeys.legacySelectedMediaId]

    suspend fun clearLegacySelectedMediaId() {
        dataStore.edit { it.remove(SettingsKeys.legacySelectedMediaId) }
    }

    private fun toAppSettings(preferences: Preferences): AppSettings {
        val defaults = AppSettings()

        return AppSettings(
            playWhenAppIsClosed = preferences.valueOr(SettingsKeys.playWhenAppIsClosed, defaults.playWhenAppIsClosed),
            openPlayerOnStartup = preferences.valueOr(SettingsKeys.openPlayerOnStartup, defaults.openPlayerOnStartup),
            reloadGuideShown = preferences.valueOr(SettingsKeys.reloadGuideShown, defaults.reloadGuideShown),
            lastBookId = preferences[SettingsKeys.lastBookId],
            librarySortOrder = enumOrDefault(preferences[SettingsKeys.librarySortOrder], defaults.librarySortOrder),
            librarySortAscending = preferences.valueOr(SettingsKeys.librarySortAscending, defaults.librarySortAscending),
            libraryTab = preferences[SettingsKeys.libraryTab]?.let { name -> LibraryTab.entries.firstOrNull { it.name == name } },
            libraryLayout = enumOrDefault(preferences[SettingsKeys.libraryLayout], defaults.libraryLayout),
            missingBookRetentionDays = preferences.valueOr(SettingsKeys.missingBookRetentionDays, defaults.missingBookRetentionDays),
            smallBackSeconds = preferences.valueOr(SettingsKeys.smallBackSeconds, defaults.smallBackSeconds),
            smallForwardSeconds = preferences.valueOr(SettingsKeys.smallForwardSeconds, defaults.smallForwardSeconds),
            largeBackSeconds = preferences.valueOr(SettingsKeys.largeBackSeconds, defaults.largeBackSeconds),
            largeForwardSeconds = preferences.valueOr(SettingsKeys.largeForwardSeconds, defaults.largeForwardSeconds),
            defaultSpeed = preferences.valueOr(SettingsKeys.defaultSpeed, defaults.defaultSpeed),
            pendingCoverBookId = preferences[SettingsKeys.pendingCoverBookId],
            themeMode = enumOrDefault(preferences[SettingsKeys.themeMode], defaults.themeMode),
            dynamicColor = preferences.valueOr(SettingsKeys.dynamicColor, defaults.dynamicColor),
            shareDiagnostics = preferences.valueOr(SettingsKeys.shareDiagnostics, defaults.shareDiagnostics),
            showAds = preferences.valueOr(SettingsKeys.showAds, defaults.showAds),
            autoStopAfterPauseMinutes = preferences.valueOr(SettingsKeys.autoStopAfterPauseMinutes, defaults.autoStopAfterPauseMinutes),
            smartRewind = enumOrDefault(preferences[SettingsKeys.smartRewind], defaults.smartRewind),
            sleepDefaultMinutes = preferences.valueOr(SettingsKeys.sleepDefaultMinutes, defaults.sleepDefaultMinutes),
            sleepFadeSeconds = preferences.valueOr(SettingsKeys.sleepFadeSeconds, defaults.sleepFadeSeconds),
            shakeToExtend = preferences.valueOr(SettingsKeys.shakeToExtend, defaults.shakeToExtend),
            shakeSensitivity = enumOrDefault(preferences[SettingsKeys.shakeSensitivity], defaults.shakeSensitivity),
            extendOnlyDuringFade = preferences.valueOr(SettingsKeys.extendOnlyDuringFade, defaults.extendOnlyDuringFade),
            sleepChime = preferences.valueOr(SettingsKeys.sleepChime, defaults.sleepChime),
            sleepChimeVolumePercent = preferences.valueOr(SettingsKeys.sleepChimeVolumePercent, defaults.sleepChimeVolumePercent),
            autoSleepEnabled = preferences.valueOr(SettingsKeys.autoSleepEnabled, defaults.autoSleepEnabled),
            autoSleepStartMinute = preferences.valueOr(SettingsKeys.autoSleepStartMinute, defaults.autoSleepStartMinute),
            autoSleepEndMinute = preferences.valueOr(SettingsKeys.autoSleepEndMinute, defaults.autoSleepEndMinute),
            saveProgressInFolders = preferences.valueOr(SettingsKeys.saveProgressInFolders, defaults.saveProgressInFolders),
            defaultSilenceTrim = enumOrDefault(preferences[SettingsKeys.defaultSilenceTrim], defaults.defaultSilenceTrim),
            defaultBoost = enumOrDefault(preferences[SettingsKeys.defaultBoost], defaults.defaultBoost),
            defaultEqualizerPresetId = preferences.valueOr(SettingsKeys.defaultEqualizerPresetId, defaults.defaultEqualizerPresetId),
            shortInterruptions = enumOrDefault(preferences[SettingsKeys.shortInterruptions], defaults.shortInterruptions),
            resumeAfterCalls = preferences.valueOr(SettingsKeys.resumeAfterCalls, defaults.resumeAfterCalls),
            resumeOnHeadsetConnect = preferences.valueOr(SettingsKeys.resumeOnHeadsetConnect, defaults.resumeOnHeadsetConnect),
            headsetNextAction = enumOrDefault(preferences[SettingsKeys.headsetNextAction], defaults.headsetNextAction),
            headsetPreviousAction = enumOrDefault(preferences[SettingsKeys.headsetPreviousAction], defaults.headsetPreviousAction),
            headsetLongPressAction = enumOrDefault(preferences[SettingsKeys.headsetLongPressAction], defaults.headsetLongPressAction),
            sleepInCar = preferences.valueOr(SettingsKeys.sleepInCar, defaults.sleepInCar),
            statisticsEnabled = preferences.valueOr(SettingsKeys.statisticsEnabled, defaults.statisticsEnabled),
            continueWithQueue = preferences.valueOr(SettingsKeys.continueWithQueue, defaults.continueWithQueue),
            faceDownToPause = preferences.valueOr(SettingsKeys.faceDownToPause, defaults.faceDownToPause),
            flipToRewind = preferences.valueOr(SettingsKeys.flipToRewind, defaults.flipToRewind),
            converterCopyCover = preferences.valueOr(SettingsKeys.converterCopyCover, defaults.converterCopyCover),
            converterMinimumBatteryPercent = preferences.valueOr(
                SettingsKeys.converterMinimumBatteryPercent,
                defaults.converterMinimumBatteryPercent,
            ),
            converterSavedBytes = preferences.valueOr(SettingsKeys.converterSavedBytes, defaults.converterSavedBytes),
            showCastButton = preferences.valueOr(SettingsKeys.showCastButton, defaults.showCastButton),
        ).withDisplaySettings(preferences)
    }

    /** The settings added with the Phase 10 settings tree. */
    private fun writeDisplaySettings(preferences: MutablePreferences, updated: AppSettings) {
        preferences[SettingsKeys.rescanOnStart] = updated.rescanOnStart.name
        preferences[SettingsKeys.skipButtonsInNotification] = updated.skipButtonsInNotification.name
        preferences[SettingsKeys.defaultNoiseReduction] = updated.defaultNoiseReduction.name
        preferences[SettingsKeys.coverOnLockScreen] = updated.coverOnLockScreen
        preferences[SettingsKeys.keepScreenOnWhileCharging] = updated.keepScreenOnWhileCharging
        preferences[SettingsKeys.progressLabels] = updated.progressLabels.name
        preferences[SettingsKeys.hiddenQuickActions] = updated.hiddenQuickActions.mapTo(mutableSetOf()) { it.name }
        preferences[SettingsKeys.language] = updated.language.name
        preferences[SettingsKeys.castProcessedAudio] = updated.castProcessedAudio
    }

    private fun AppSettings.withDisplaySettings(preferences: Preferences): AppSettings = copy(
        rescanOnStart = enumOrDefault(preferences[SettingsKeys.rescanOnStart], rescanOnStart),
        skipButtonsInNotification = enumOrDefault(preferences[SettingsKeys.skipButtonsInNotification], skipButtonsInNotification),
        defaultNoiseReduction = enumOrDefault(preferences[SettingsKeys.defaultNoiseReduction], defaultNoiseReduction),
        coverOnLockScreen = preferences.valueOr(SettingsKeys.coverOnLockScreen, coverOnLockScreen),
        keepScreenOnWhileCharging = preferences.valueOr(SettingsKeys.keepScreenOnWhileCharging, keepScreenOnWhileCharging),
        progressLabels = enumOrDefault(preferences[SettingsKeys.progressLabels], progressLabels),
        hiddenQuickActions = preferences[SettingsKeys.hiddenQuickActions].orEmpty()
            .mapNotNullTo(mutableSetOf()) { name -> QuickAction.entries.firstOrNull { it.name == name } },
        language = enumOrDefault(preferences[SettingsKeys.language], language),
        castProcessedAudio = preferences.valueOr(SettingsKeys.castProcessedAudio, castProcessedAudio),
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private fun <T> Preferences.valueOr(key: Preferences.Key<T>, default: T): T = this[key] ?: default
}
