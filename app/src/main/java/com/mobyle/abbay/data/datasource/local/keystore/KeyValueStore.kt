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
}
