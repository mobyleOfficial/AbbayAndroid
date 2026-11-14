package com.mobyle.abbay.data.settings

import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences

internal fun legacyPreferencesMigration(context: Context) = SharedPreferencesMigration<Preferences>(
    context = context,
    sharedPreferencesName = LEGACY_PREFERENCES_NAME,
    keysToMigrate = LEGACY_KEYS,
) { legacyPreferences, currentPreferences ->
    val migrated = currentPreferences.toMutablePreferences()
    val legacyValues = legacyPreferences.getAll()

    (legacyValues[PLAY_WHEN_APP_IS_CLOSED] as? Boolean)?.let { migrated[SettingsKeys.playWhenAppIsClosed] = it }
    (legacyValues[OPEN_PLAYER_IN_STARTUP] as? Boolean)?.let { migrated[SettingsKeys.openPlayerOnStartup] = it }
    (legacyValues[RELOAD_GUIDE] as? Boolean)?.let { migrated[SettingsKeys.reloadGuideShown] = it }
    (legacyValues[BOOK_FOLDER_PATH] as? String)?.let { migrated[SettingsKeys.legacyFolderUri] = it }
    (legacyValues[LAST_SELECTED_BOOK_ID] as? String)?.let { migrated[SettingsKeys.legacySelectedMediaId] = it }

    migrated.toPreferences()
}

private const val LEGACY_PREFERENCES_NAME = "Prefs"
private const val PLAY_WHEN_APP_IS_CLOSED = "PLAY_WHEN_APP_IS_CLOSED"
private const val OPEN_PLAYER_IN_STARTUP = "OPEN_PLAYER_IN_STARTUP"
private const val RELOAD_GUIDE = "RELOAD_GUIDE"
private const val BOOK_FOLDER_PATH = "BOOK_FOLDER_PATH"
private const val LAST_SELECTED_BOOK_ID = "LAST_SELECTED_BOOK_ID"

private val LEGACY_KEYS = setOf(
    PLAY_WHEN_APP_IS_CLOSED,
    OPEN_PLAYER_IN_STARTUP,
    RELOAD_GUIDE,
    BOOK_FOLDER_PATH,
    LAST_SELECTED_BOOK_ID,
    "APP_LIFE_STATUS",
    "BOOK_POSITION",
)
