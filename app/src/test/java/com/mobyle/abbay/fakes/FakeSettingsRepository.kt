package com.mobyle.abbay.fakes

import com.repository.SettingsRepository
import com.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initialSettings: AppSettings = AppSettings()) : SettingsRepository {
    val settings = MutableStateFlow(initialSettings)

    override fun observeSettings(): Flow<AppSettings> = settings

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
}
