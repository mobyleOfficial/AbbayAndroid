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

    fun getStringStoredValue(key: KeyValueStoreKeys): String? {
        return sharedPrefs.getString(key.name, null)
    }

    fun storeStringValue(key: KeyValueStoreKeys, value: String?) {
        sharedPrefs.edit {
            putString(key.name, value)
        }
    }

    fun deleteAllBookInformation() {
        sharedPrefs.edit {
            remove(KeyValueStoreKeys.LAST_SELECTED_BOOK_ID.name)
        }
    }

    companion object {
        const val KEY = "Prefs"
    }
}