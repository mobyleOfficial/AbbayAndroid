package com.mobyle.abbay.data.datasource.local.keystore

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyValueStore @Inject constructor(private val prefs: SharedPreferences) {
}
