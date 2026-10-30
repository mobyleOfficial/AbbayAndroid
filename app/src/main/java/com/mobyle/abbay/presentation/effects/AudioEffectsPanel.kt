package com.mobyle.abbay.presentation.effects

import com.effects.AudioEffects
import com.effects.EqualizerPreset

class AudioEffectsPanel(
    val effects: AudioEffects? = null,
    val presets: List<EqualizerPreset> = EqualizerPreset.BUILT_IN,
    val onChange: (AudioEffects) -> Unit = {},
    val onApplyEqualizerToAll: (List<Int>) -> Unit = {},
    val onSavePreset: (String, List<Int>) -> Unit = { _, _ -> },
    val onDeletePreset: (Long) -> Unit = {},
    val isCasting: Boolean = false,
)
