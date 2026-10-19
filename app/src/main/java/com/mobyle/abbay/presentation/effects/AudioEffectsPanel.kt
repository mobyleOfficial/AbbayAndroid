package com.mobyle.abbay.presentation.effects

import com.effects.AudioEffects
import com.effects.EqualizerPreset

/** What the player needs to show and change the audio effects; built by the screen from [AudioEffectsViewModel]. */
class AudioEffectsPanel(
    val effects: AudioEffects? = null,
    val presets: List<EqualizerPreset> = EqualizerPreset.BUILT_IN,
    val onChange: (AudioEffects) -> Unit = {},
    val onApplyEqualizerToAll: (List<Int>) -> Unit = {},
    val onSavePreset: (String, List<Int>) -> Unit = { _, _ -> },
    val onDeletePreset: (Long) -> Unit = {},
    /** Casting: the receiver gets the original files, so the sheet says the effects do not apply. */
    val isCasting: Boolean = false,
)
