package com.settings

import com.effects.BoostLevel
import com.effects.NoiseReductionLevel
import com.effects.SilenceTrimLevel
import com.library.LibraryLayout
import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.listening.ShakeSensitivity
import com.listening.SmartRewindLevel

data class AppSettings(
    val playWhenAppIsClosed: Boolean = false,
    val openPlayerOnStartup: Boolean = false,
    val reloadGuideShown: Boolean = false,
    val lastBookId: String? = null,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_PLAYED,
    val librarySortAscending: Boolean = false,
    val libraryTab: LibraryTab? = null,
    val libraryTabOrder: List<LibraryTab> = LibraryTab.entries,
    val hiddenLibraryTabs: Set<LibraryTab> = emptySet(),
    val libraryLayout: LibraryLayout = LibraryLayout.LIST,
    val missingBookRetentionDays: Int = 30,
    val smallBackSeconds: Int = 15,
    val smallForwardSeconds: Int = 30,
    val largeBackSeconds: Int = 300,
    val largeForwardSeconds: Int = 300,
    val defaultSpeed: Float = 1f,
    val pendingCoverBookId: String? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val shareDiagnostics: Boolean = true,
    val autoStopAfterPauseMinutes: Int = 30,
    val smartRewind: SmartRewindLevel = SmartRewindLevel.STANDARD,
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
    val saveProgressInFolders: Boolean = true,
    val defaultSilenceTrim: SilenceTrimLevel = SilenceTrimLevel.OFF,
    val defaultBoost: BoostLevel = BoostLevel.OFF,
    val defaultEqualizerPresetId: Long = -1L,
    val shortInterruptions: ShortInterruptionBehavior = ShortInterruptionBehavior.PAUSE,
    val resumeAfterCalls: Boolean = true,
    val resumeOnHeadsetConnect: Boolean = false,
    val headsetNextAction: HeadsetSkipAction = HeadsetSkipAction.SKIP,
    val headsetPreviousAction: HeadsetSkipAction = HeadsetSkipAction.SKIP,
    val headsetLongPressAction: HeadsetLongPressAction = HeadsetLongPressAction.BOOKMARK,
    val sleepInCar: Boolean = false,
    val statisticsEnabled: Boolean = true,
    val continueWithQueue: Boolean = true,
    val faceDownToPause: Boolean = false,
    val flipToRewind: Boolean = false,
    val converterCopyCover: Boolean = true,
    val converterMinimumBatteryPercent: Int = 50,
    val converterSavedBytes: Long = 0L,
    val showCastButton: Boolean = true,
    val rescanOnStart: RescanOnStart = RescanOnStart.QUICK,
    val skipButtonsInNotification: NotificationSkipButtons = NotificationSkipButtons.TIME_SKIP,
    val defaultNoiseReduction: NoiseReductionLevel = NoiseReductionLevel.OFF,
    val coverOnLockScreen: Boolean = true,
    val keepScreenOnWhileCharging: Boolean = false,
    val progressLabels: ProgressLabels = ProgressLabels.ELAPSED_AND_REMAINING,
    val hiddenQuickActions: Set<QuickAction> = emptySet(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    val castProcessedAudio: Boolean = true,
) {
    val visibleLibraryTabs: List<LibraryTab> get() = LibraryTab.visible(libraryTabOrder, hiddenLibraryTabs)

    companion object {
        const val DEFAULT_AUTO_SLEEP_START_MINUTE = 22 * 60
        const val DEFAULT_AUTO_SLEEP_END_MINUTE = 6 * 60
    }
}
