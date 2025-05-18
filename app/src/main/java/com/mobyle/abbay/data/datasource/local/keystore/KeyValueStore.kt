package com.mobyle.abbay.data.datasource.local.keystore

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class KeyValueStore @Inject constructor(private val sharedPrefs: SharedPreferences) {
    fun getBooleanStoredValue(key: KeyValueStoreKeys): Boolean {
        return sharedPrefs.getBoolean(key.name, false)
    }

    fun storeBooleanValue(key: KeyValueStoreKeys, value: Boolean) {
        sharedPrefs.edit {
            putBoolean(key.name, value)
        }
    }

    fun deleteAllBookInformation() {
        sharedPrefs.edit {
            remove(LAST_SELECTED_BOOK_ID)
        }
    }

    companion object {
        const val KEY = "Prefs"
        const val LAST_SELECTED_BOOK_ID = "LAST_SELECTED_BOOK_ID"
    }
}