package com.usecase

import com.repository.ReadAlongRepository
import com.repository.SettingsRepository
import javax.inject.Inject

/** Turns Read-along on or off. Off deletes every transcript and speech model: nothing it made or fetched stays behind. */
class SetReadAlongEnabled @Inject constructor(
    private val settings: SettingsRepository,
    private val readAlong: ReadAlongRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        settings.updateSettings { it.copy(readAlongEnabled = enabled) }
        if (!enabled) {
            readAlong.deleteAll()
            readAlong.deleteModels()
        }
    }
}
