package com.repository

import com.effects.AudioEffects
import com.effects.EqualizerPreset
import kotlinx.coroutines.flow.Flow

interface AudioEffectsRepository {
    fun observeEffects(bookId: String): Flow<AudioEffects>

    suspend fun updateEffects(bookId: String, effects: AudioEffects)

    suspend fun applyEqualizerToAllBooks(gains: List<Int>)

    fun observePresets(): Flow<List<EqualizerPreset>>

    suspend fun savePreset(name: String, gains: List<Int>): Long

    suspend fun deletePreset(presetId: Long)

    suspend fun addSilenceSaved(bookId: String, milliseconds: Long)
}
