package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.settings.SettingsDataSource
import com.repository.SettingsRepository
import com.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(private val dataSource: SettingsDataSource) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = dataSource.observeSettings()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = dataSource.updateSettings(transform)
}
