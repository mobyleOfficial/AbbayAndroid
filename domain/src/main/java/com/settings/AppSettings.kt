package com.settings

import com.effects.BoostLevel
import com.effects.NoiseReductionLevel
import com.effects.SilenceTrimLevel
import com.library.LibraryLayout
import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.listening.ShakeSensitivity
import com.listening.SmartRewindLevel

/** App preferences stored in DataStore. */
data class AppSettings(
    val playWhenAppIsClosed: Boolean = false,
    val openPlayerOnStartup: Boolean = false,
    val reloadGuideShown: Boolean = false,
    val lastBookId: String? = null,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_PLAYED,
    val librarySortAscending: Boolean = false,
    /** null means "auto": In progress when it is non-empty, otherwise All. */
    val libraryTab: LibraryTab? = null,
    val libraryTabOrder: List<LibraryTab> = LibraryTab.entries,
    val hiddenLibraryTabs: Set<LibraryTab> = emptySet(),
    val libraryLayout: LibraryLayout = LibraryLayout.LIST,
    val missingBookRetentionDays: Int = 30,
    /** Small skips of the notification and headset. */
    val smallBackSeconds: Int = 15,
    val smallForwardSeconds: Int = 30,
    val largeBackSeconds: Int = 300,
    val largeForwardSeconds: Int = 300,
    /** Speed of books that were never played. */
    val defaultSpeed: Float = 1f,
    /** Book waiting for an image shared back from the browser ("Search the web"). */
    val pendingCoverBookId: String? = null,
    /** Dark is the AbbayAndroid look and stays the default. */
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Wallpaper colors on Android 12+; off by default to keep the AbbayAndroid look. */
    val dynamicColor: Boolean = false,
    /** "Send crash reports and usage statistics". */
    val shareDiagnostics: Boolean = true,
    /** Paused this long, the service stops itself; 0 means never. */
    val autoStopAfterPauseMinutes: Int = 30,
    /** Rewind on resume after a pause. */
    val smartRewind: SmartRewindLevel = SmartRewindLevel.STANDARD,
    /** Sleep timer settings. */
    val sleepDefaultMinutes: Int = 15,
    val sleepFadeSeconds: Int = 30,
    val shakeToExtend: Boolean = true,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    val extendOnlyDuringFade: Boolean = false,
    val sleepChime: Boolean = false,
    val sleepChimeVolumePercent: Int = 50,
    val autoSleepEnabled: Boolean = false,
    val autoSleepStartMinute: Int = DEFAULT_AUTO_SLEEP_START_MINUTE,
    val autoSleepEndMinute: Int = DEFAULT_AUTO_SLEEP_END_MINUTE,
    /** "Save progress in book folders": writes `.abbay/book.json`; reading always happens. */
    val saveProgressInFolders: Boolean = true,
    /** Effects of books that were never played (Audio). */
    val defaultSilenceTrim: SilenceTrimLevel = SilenceTrimLevel.OFF,
    val defaultBoost: BoostLevel = BoostLevel.OFF,
    /** Id of an [com.effects.EqualizerPreset]; built-in presets have negative ids. */
    val defaultEqualizerPresetId: Long = -1L,
    /** Controls. */
    val shortInterruptions: ShortInterruptionBehavior = ShortInterruptionBehavior.PAUSE,
    val resumeAfterCalls: Boolean = true,
    val resumeOnHeadsetConnect: Boolean = false,
    val headsetNextAction: HeadsetSkipAction = HeadsetSkipAction.SKIP,
    val headsetPreviousAction: HeadsetSkipAction = HeadsetSkipAction.SKIP,
    val headsetLongPressAction: HeadsetLongPressAction = HeadsetLongPressAction.BOOKMARK,
    /** "Sleep timer in car": off keeps the timer away from Android Auto. */
    val sleepInCar: Boolean = false,
    /** Statistics record listening time. */
    val statisticsEnabled: Boolean = true,
    /** A finished book starts the next queued one. */
    val continueWithQueue: Boolean = true,
    /** Motion gestures, each off by default. */
    val faceDownToPause: Boolean = false,
    val flipToRewind: Boolean = false,
    /** Converter: copy the embedded cover, battery level needed when not charging. */
    val converterCopyCover: Boolean = true,
    val converterMinimumBatteryPercent: Int = 50,
    /** Bytes saved by every conversion so far. */
    val converterSavedBytes: Long = 0L,
    /** The Cast button in the player. */
    val showCastButton: Boolean = true,
    /** Settings tree completed in Phase 10. */
    val rescanOnStart: RescanOnStart = RescanOnStart.QUICK,
    val skipButtonsInNotification: NotificationSkipButtons = NotificationSkipButtons.TIME_SKIP,
    val defaultNoiseReduction: NoiseReductionLevel = NoiseReductionLevel.OFF,
    val coverOnLockScreen: Boolean = true,
    val keepScreenOnWhileCharging: Boolean = false,
    val progressLabels: ProgressLabels = ProgressLabels.ELAPSED_AND_REMAINING,
    val hiddenQuickActions: Set<QuickAction> = emptySet(),
    /** Mirror of the per-app language (kept by the platform or the app); services rebuild their texts when it changes. */
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Cast phase 2: send the audio with its effects instead of the original files. */
    val castProcessedAudio: Boolean = true,
) {
    val visibleLibraryTabs: List<LibraryTab> get() = LibraryTab.visible(libraryTabOrder, hiddenLibraryTabs)

    companion object {
        /** 22:00–06:00, the example window. */
        const val DEFAULT_AUTO_SLEEP_START_MINUTE = 22 * 60
        const val DEFAULT_AUTO_SLEEP_END_MINUTE = 6 * 60
    }
}
