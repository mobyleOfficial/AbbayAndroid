package com.mobyle.abbay.data.effects

import com.effects.AudioEffects
import com.effects.EqualizerPreset
import com.mobyle.abbay.data.database.dao.AudioEffectsDao
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.settings.SettingsDataSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** The effects a book starts with: the Audio defaults of the settings. */
class EffectDefaults @Inject constructor(private val settings: SettingsDataSource, private val dao: AudioEffectsDao) {
    suspend fun effects(): AudioEffects {
        val current = settings.observeSettings().first()
        return AudioEffects(
            silenceTrim = current.defaultSilenceTrim,
            boost = current.defaultBoost,
            noiseReduction = current.defaultNoiseReduction,
            equalizerGains = presetGains(current.defaultEqualizerPresetId),
        )
    }

    /** A new `listening_state` row for [bookId] with the default effects. */
    suspend fun newListeningState(bookId: Long): ListeningStateEntity =
        EffectsMapper.withEffects(ListeningStateEntity(bookId = bookId), effects())

    private suspend fun presetGains(presetId: Long): List<Int> =
        EqualizerPreset.BUILT_IN.firstOrNull { it.id == presetId }?.gains
            ?: dao.observePresets().first().firstOrNull { it.id == presetId }?.let { EffectsMapper.gainsOf(it.gainsJson) }
            ?: AudioEffects.FLAT
}
