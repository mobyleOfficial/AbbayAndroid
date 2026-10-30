package com.mobyle.abbay.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.effects.NoiseReductionLevel
import com.settings.AppLanguage
import com.settings.AppSettings
import com.settings.NotificationSkipButtons
import com.settings.ProgressLabels
import com.settings.QuickAction
import com.settings.RescanOnStart
import com.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun appearancePrivacyAndCoverSettingsSurviveARoundTrip() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb") }
        val dataSource = SettingsDataSource(dataStore)

        assertEquals(AppSettings(), dataSource.observeSettings().first())

        dataSource.updateSettings {
            it.copy(
                themeMode = ThemeMode.BLACK,
                dynamicColor = true,
                shareDiagnostics = false,
                pendingCoverBookId = "7",
                defaultSpeed = 1.25f
            )
        }
        val stored = dataSource.observeSettings().first()
        assertEquals(ThemeMode.BLACK, stored.themeMode)
        assertEquals(true, stored.dynamicColor)
        assertEquals(false, stored.shareDiagnostics)
        assertEquals("7", stored.pendingCoverBookId)
        assertEquals(1.25f, stored.defaultSpeed)

        dataSource.updateSettings { it.copy(pendingCoverBookId = null) }
        assertEquals(null, dataSource.observeSettings().first().pendingCoverBookId)
        scope.cancel()
    }

    @Test
    fun phaseTenSettingsSurviveARoundTrip() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("settings.preferences_pb") }
        val dataSource = SettingsDataSource(dataStore)
        val changed = AppSettings().copy(
            rescanOnStart = RescanOnStart.FULL,
            skipButtonsInNotification = NotificationSkipButtons.PREVIOUS_AND_NEXT_CHAPTER,
            defaultNoiseReduction = NoiseReductionLevel.MEDIUM,
            coverOnLockScreen = false,
            keepScreenOnWhileCharging = true,
            progressLabels = ProgressLabels.ELAPSED_AND_TOTAL,
            hiddenQuickActions = setOf(QuickAction.REPEAT, QuickAction.SKIP_INTRO_OUTRO),
            language = AppLanguage.PORTUGUESE,
            castProcessedAudio = false,
        )

        dataSource.updateSettings { changed }

        assertEquals(changed, dataSource.observeSettings().first())
        scope.cancel()
    }
}
