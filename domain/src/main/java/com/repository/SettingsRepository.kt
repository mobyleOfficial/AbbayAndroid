package com.repository

import com.settings.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}
