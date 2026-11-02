package com.usecase

import com.repository.SettingsRepository
import javax.inject.Inject

/** Whether the player's cover square shows the text being read rather than the cover. */
class SetReadAlongShowsText @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(showsText: Boolean) = settings.updateSettings { it.copy(readAlongShowsText = showsText) }
}
