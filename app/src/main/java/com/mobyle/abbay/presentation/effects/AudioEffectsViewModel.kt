package com.mobyle.abbay.presentation.effects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.effects.AudioEffects
import com.effects.EqualizerPreset
import com.usecase.ApplyEqualizerToAllBooks
import com.usecase.DeleteEqualizerPreset
import com.usecase.ObserveAudioEffects
import com.usecase.ObserveEqualizerPresets
import com.usecase.SaveEqualizerPreset
import com.usecase.UpdateAudioEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Audio effects of the book in the player, for the audio sheet. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AudioEffectsViewModel @Inject constructor(
    observeAudioEffects: ObserveAudioEffects,
    observeEqualizerPresets: ObserveEqualizerPresets,
    private val updateAudioEffects: UpdateAudioEffects,
    private val applyEqualizerToAllBooks: ApplyEqualizerToAllBooks,
    private val saveEqualizerPreset: SaveEqualizerPreset,
    private val deleteEqualizerPreset: DeleteEqualizerPreset,
) : ViewModel() {
    private val bookId = MutableStateFlow<String?>(null)

    /** Null while no book is loaded. */
    val effects: StateFlow<AudioEffects?> = bookId
        .flatMapLatest { id -> id?.let { observeAudioEffects(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val presets: StateFlow<List<EqualizerPreset>> = observeEqualizerPresets()
        .stateIn(viewModelScope, SharingStarted.Eagerly, EqualizerPreset.BUILT_IN)

    fun load(bookId: String?) {
        this.bookId.value = bookId
    }

    fun change(effects: AudioEffects) {
        val id = bookId.value ?: return
        viewModelScope.launch { updateAudioEffects(id, effects) }
    }

    fun applyEqualizerToAll(gains: List<Int>) = viewModelScope.launch { applyEqualizerToAllBooks(gains) }

    fun savePreset(name: String, gains: List<Int>) = viewModelScope.launch { saveEqualizerPreset(name, gains) }

    fun deletePreset(presetId: Long) = viewModelScope.launch { deleteEqualizerPreset(presetId) }
}
