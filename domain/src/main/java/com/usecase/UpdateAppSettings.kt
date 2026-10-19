package com.usecase

import com.repository.SettingsRepository
import com.settings.AppSettings
import javax.inject.Inject

class UpdateAppSettings @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) = repository.updateSettings(transform)
}
