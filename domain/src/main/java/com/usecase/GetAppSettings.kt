package com.usecase

import com.repository.SettingsRepository
import com.settings.AppSettings
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetAppSettings @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(): AppSettings = repository.observeSettings().first()
}
