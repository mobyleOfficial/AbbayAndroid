package com.repository

import com.effects.AudioEffects
import com.effects.EqualizerPreset
import kotlinx.coroutines.flow.Flow

/** Per-book audio effects and user equalizer presets. */
interface AudioEffectsRepository {
    /** Effects of [bookId]; a book never played yet shows the defaults of the settings. */
    fun observeEffects(bookId: String): Flow<AudioEffects>

    suspend fun updateEffects(bookId: String, effects: AudioEffects)

    /** "Apply to all books" of the equalizer sheet. */
    suspend fun applyEqualizerToAllBooks(gains: List<Int>)

    /** Built-in presets first, then the user's, by name. */
    fun observePresets(): Flow<List<EqualizerPreset>>

    suspend fun savePreset(name: String, gains: List<Int>): Long

    suspend fun deletePreset(presetId: Long)

    /** Adds time saved by silence trimming to the book's total. */
    suspend fun addSilenceSaved(bookId: String, milliseconds: Long)
}
