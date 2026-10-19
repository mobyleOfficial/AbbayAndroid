package com.mobyle.abbay.presentation.effects

import com.effects.BoostLevel
import com.effects.NoiseReductionLevel
import com.effects.SilenceTrimLevel
import com.mobyle.abbay.R

internal fun SilenceTrimLevel.labelResource(): Int = when (this) {
    SilenceTrimLevel.OFF -> R.string.effects_off
    SilenceTrimLevel.GENTLE -> R.string.effects_trim_gentle
    SilenceTrimLevel.MODERATE -> R.string.effects_trim_moderate
    SilenceTrimLevel.STRONG -> R.string.effects_trim_strong
}

internal fun NoiseReductionLevel.labelResource(): Int = when (this) {
    NoiseReductionLevel.OFF -> R.string.effects_off
    NoiseReductionLevel.LOW -> R.string.effects_boost_low
    NoiseReductionLevel.MEDIUM -> R.string.effects_boost_medium
    NoiseReductionLevel.HIGH -> R.string.effects_boost_high
}

internal fun BoostLevel.labelResource(): Int = when (this) {
    BoostLevel.OFF -> R.string.effects_off
    BoostLevel.LOW -> R.string.effects_boost_low
    BoostLevel.MEDIUM -> R.string.effects_boost_medium
    BoostLevel.HIGH -> R.string.effects_boost_high
}
