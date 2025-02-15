package com.usecase

import com.repository.SettingsRepository
import javax.inject.Inject

class EnableOpenPlayerInStartup @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke() = repository.enableOpenPlayerInStartup()
}