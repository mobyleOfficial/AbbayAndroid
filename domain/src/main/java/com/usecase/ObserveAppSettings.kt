package com.usecase

import com.repository.SettingsRepository
import javax.inject.Inject

class ObserveAppSettings @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke() = repository.observeSettings()
}
