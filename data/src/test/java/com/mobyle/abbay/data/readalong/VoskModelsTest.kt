package com.mobyle.abbay.data.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoskModelsTest {
    @Test
    fun `every model is pinned to an https archive, a size and a sha-256`() {
        VoskModels.all.forEach { model ->
            assertTrue(model.url, model.url.startsWith("https://alphacephei.com/vosk/models/"))
            assertTrue(model.archive, model.sizeBytes > 0)
            assertTrue(model.archive, model.sha256.matches(Regex("[0-9a-f]{64}")))
        }
        assertEquals(VoskModels.all.size, VoskModels.all.map { it.locale }.toSet().size)
    }

    @Test
    fun `a locale finds its model, or its language's`() {
        assertEquals("pt-BR", VoskModels.forLocale("pt-BR")?.locale)
        assertEquals("pt-BR", VoskModels.forLocale("pt_PT")?.locale)
        assertEquals("en-US", VoskModels.forLocale("en-GB")?.locale)
        assertNull(VoskModels.forLocale("ja-JP"))
    }
}
