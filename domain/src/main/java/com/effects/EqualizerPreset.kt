package com.effects

data class EqualizerPreset(val id: Long, val name: String, val gains: List<Int>, val isBuiltIn: Boolean) {
    companion object {
        val FLAT = EqualizerPreset(-1, "flat", AudioEffects.FLAT, isBuiltIn = true)
        val VOICE_CLARITY = EqualizerPreset(-2, "voiceClarity", listOf(-6, -4, -2, 0, 0, 0, 3, 3, 0, 0), isBuiltIn = true)
        val REDUCE_HISS = EqualizerPreset(-3, "reduceHiss", listOf(0, 0, 0, 0, 0, 0, 0, 0, -4, -8), isBuiltIn = true)
        val WARM = EqualizerPreset(-4, "warm", listOf(0, 0, 2, 2, 0, 0, 0, 0, -2, 0), isBuiltIn = true)
        val SMALL_SPEAKER = EqualizerPreset(-5, "smallSpeaker", listOf(-8, -8, 0, 0, 0, 3, 3, 0, 0, 0), isBuiltIn = true)
        val BUILT_IN = listOf(FLAT, VOICE_CLARITY, REDUCE_HISS, WARM, SMALL_SPEAKER)
    }
}
