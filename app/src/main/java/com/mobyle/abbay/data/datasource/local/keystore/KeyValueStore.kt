package com.mobyle.abbay.data.datasource.local.keystore

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyValueStore @Inject constructor(private val prefs: SharedPreferences) {
    fun setAlreadyAskedPermission() {
        prefs.edit {
            putBoolean(HAS_ALREADY_ASKED_PERMISSION, true)
        }
    }

    fun getCurrentCategoryId(): Boolean =
        prefs.getBoolean(HAS_ALREADY_ASKED_PERMISSION, false)

    companion object {
        private const val HAS_ALREADY_ASKED_PERMISSION = "has_already_asked_permission"
    }
}
