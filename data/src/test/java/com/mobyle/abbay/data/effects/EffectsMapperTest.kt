package com.mobyle.abbay.data.effects

import com.effects.AudioEffects
import com.effects.BoostLevel
import com.effects.SilenceTrimLevel
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EffectsMapperTest {
    @Test
    fun effectsRoundTripThroughTheListeningState() {
        val effects = AudioEffects(
            silenceTrim = SilenceTrimLevel.STRONG,
            boost = BoostLevel.HIGH,
            equalizerGains = listOf(-6, -4, -2, 0, 0, 0, 3, 3, 0, 0),
            mono = true,
            balance = -0.25f,
            pitchSemitones = 1.5f,
        )
        val state = EffectsMapper.withEffects(ListeningStateEntity(bookId = 1, silenceSavedMs = 42L), effects)

        assertEquals(effects.copy(silenceSavedMs = 42L), EffectsMapper.toEffects(state))
        assertEquals("[-6,-4,-2,0,0,0,3,3,0,0]", state.equalizerJson)
    }

    @Test
    fun flatIsStoredAsNullAndOddJsonIsTolerated() {
        assertNull(EffectsMapper.jsonOf(AudioEffects.FLAT))
        assertEquals(AudioEffects.FLAT, EffectsMapper.gainsOf(null))
        // The sidecar example has spaces; short arrays are padded with 0.
        assertEquals(listOf(0, 0, -2, 0, 0, 0, 3, 3, 0, 0), EffectsMapper.gainsOf("[0, 0, -2, 0, 0, 0, 3, 3]"))
        assertEquals(
            SilenceTrimLevel.OFF,
            EffectsMapper.toEffects(ListeningStateEntity(bookId = 1, silenceTrimLevel = "FUTURE")).silenceTrim
        )
    }
}
