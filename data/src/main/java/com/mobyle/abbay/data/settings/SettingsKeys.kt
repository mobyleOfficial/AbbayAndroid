package com.mobyle.abbay.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/** DataStore keys. Names where the spec defines them. */
internal object SettingsKeys {
    val playWhenAppIsClosed = booleanPreferencesKey("playWhenAppIsClosed")
    val openPlayerOnStartup = booleanPreferencesKey("openPlayerOnStartup")
    val reloadGuideShown = booleanPreferencesKey("reloadGuideShown")
    val lastBookId = stringPreferencesKey("lastBookId")
    val librarySortOrder = stringPreferencesKey("librarySortOrder")
    val librarySortAscending = booleanPreferencesKey("librarySortAscending")
    val libraryTab = stringPreferencesKey("defaultTab")
    val libraryLayout = stringPreferencesKey("layout")
    val missingBookRetentionDays = intPreferencesKey("missingBookRetentionDays")
    val smallBackSeconds = intPreferencesKey("smallBack")
    val smallForwardSeconds = intPreferencesKey("smallForward")
    val largeBackSeconds = intPreferencesKey("largeBack")
    val largeForwardSeconds = intPreferencesKey("largeForward")
    val defaultSpeed = floatPreferencesKey("defaultSpeed")
    val pendingCoverBookId = stringPreferencesKey("pendingCoverBookId")
    val themeMode = stringPreferencesKey("themeMode")
    val dynamicColor = booleanPreferencesKey("dynamicColor")
    val shareDiagnostics = booleanPreferencesKey("shareDiagnostics")
    val autoStopAfterPauseMinutes = intPreferencesKey("autoStopAfterPauseMin")
    val smartRewind = stringPreferencesKey("smartRewind")
    val sleepDefaultMinutes = intPreferencesKey("sleepDefaultMinutes")
    val sleepFadeSeconds = intPreferencesKey("fadeSeconds")
    val shakeToExtend = booleanPreferencesKey("shakeToExtend")
    val shakeSensitivity = stringPreferencesKey("shakeSensitivity")
    val extendOnlyDuringFade = booleanPreferencesKey("extendOnlyDuringFade")
    val sleepChime = booleanPreferencesKey("chime")
    val sleepChimeVolumePercent = intPreferencesKey("chimeVolume")
    val autoSleepEnabled = booleanPreferencesKey("autoSleepSchedule")
    val autoSleepStartMinute = intPreferencesKey("autoSleepStartMinute")
    val autoSleepEndMinute = intPreferencesKey("autoSleepEndMinute")
    val saveProgressInFolders = booleanPreferencesKey("saveProgressInFolders")
    val defaultSilenceTrim = stringPreferencesKey("defaultSilenceTrim")
    val defaultBoost = stringPreferencesKey("defaultBoost")
    val defaultEqualizerPresetId = longPreferencesKey("defaultEqualizerPreset")
    val shortInterruptions = stringPreferencesKey("shortInterruptions")
    val resumeAfterCalls = booleanPreferencesKey("resumeAfterCalls")
    val resumeOnHeadsetConnect = booleanPreferencesKey("resumeOnHeadsetConnect")
    val headsetNextAction = stringPreferencesKey("headsetNext")
    val headsetPreviousAction = stringPreferencesKey("headsetPrevious")
    val headsetLongPressAction = stringPreferencesKey("headsetLongPress")
    val sleepInCar = booleanPreferencesKey("sleepInCar")
    val statisticsEnabled = booleanPreferencesKey("statisticsEnabled")
    val continueWithQueue = booleanPreferencesKey("continueWithQueue")
    val faceDownToPause = booleanPreferencesKey("faceDownToPause")
    val flipToRewind = booleanPreferencesKey("flipToRewind")
    val converterCopyCover = booleanPreferencesKey("converterCopyCover")
    val converterMinimumBatteryPercent = intPreferencesKey("converterMinimumBattery")
    val converterSavedBytes = longPreferencesKey("converterSavedBytes")
    val showCastButton = booleanPreferencesKey("showCastButton")
    val rescanOnStart = stringPreferencesKey("rescanOnStart")
    val skipButtonsInNotification = stringPreferencesKey("skipButtonsInNotification")
    val defaultNoiseReduction = stringPreferencesKey("defaultNoiseReduction")
    val coverOnLockScreen = booleanPreferencesKey("coverOnLockScreen")
    val keepScreenOnWhileCharging = booleanPreferencesKey("keepScreenOnWhileCharging")
    val progressLabels = stringPreferencesKey("progressLabels")
    val hiddenQuickActions = stringSetPreferencesKey("hiddenQuickActions")
    val language = stringPreferencesKey("language")
    val castProcessedAudio = booleanPreferencesKey("castProcessedAudio")

    /** Tab names joined by commas: a string set would not keep the order. */
    val libraryTabOrder = stringPreferencesKey("libraryTabOrder")
    val hiddenLibraryTabs = stringSetPreferencesKey("hiddenLibraryTabs")

    /** One key per book: subtitle charsets are rare, so they do not get a column. */
    fun subtitleCharset(bookId: Long) = stringPreferencesKey("subtitleCharset.$bookId")

    // Values carried over from the previous app version, consumed by the legacy migration.
    val legacyFolderUri = stringPreferencesKey("legacyFolderUri")
    val legacyFolderMigrated = booleanPreferencesKey("legacyFolderMigrated")
    val legacySelectedMediaId = stringPreferencesKey("legacySelectedMediaId")
}
