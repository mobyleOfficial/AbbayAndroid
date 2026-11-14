package com.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ReadAlongPiecesTest {
    private fun words(vararg gapsMs: Long): List<ReadAlongWord> {
        var time = 0L
        return (0..gapsMs.size).map { index ->
            val word = ReadAlongWord("w$index", time, time + 200)
            time += 200 + gapsMs.getOrElse(index) { 0L }
            word
        }
    }

    @Test
    fun `a line ends at a breath once it has enough words`() {
        val lines = ReadAlongLineBreaker.lines(words(10, 10, 10, 400, 10, 10))

        assertEquals(listOf(4, 3), lines.map { it.words.size })
    }

    @Test
    fun `a breath too early does not cut the line`() {
        val lines = ReadAlongLineBreaker.lines(words(400, 10, 10, 10))

        assertEquals(listOf(5), lines.map { it.words.size })
    }

    @Test
    fun `a long run of words is cut at its widest late gap`() {
        val gaps = LongArray(ReadAlongLineBreaker.MAX_WORDS + 2) { 10L }.also { it[8] = 250L }
        val lines = ReadAlongLineBreaker.lines(words(*gaps))

        assertEquals(9, lines.first().words.size)
        assertTrue(lines.all { it.words.size <= ReadAlongLineBreaker.MAX_WORDS })
        assertEquals(gaps.size + 1, lines.sumOf { it.words.size })
    }

    @Test
    fun `readings start in the right file and stop before a missing one`() {
        val source = ReadAlongAudioSource(
            segments = listOf(
                ReadAlongAudioSource.Segment("file:/a", 0, 10_000),
                ReadAlongAudioSource.Segment("file:/b", 10_000, 10_000),
                ReadAlongAudioSource.Segment(null, 20_000, 10_000),
                ReadAlongAudioSource.Segment("file:/d", 30_000, 10_000),
            ),
            isDownloadable = true,
        )

        val readings = source.readings(4_000)

        assertEquals(listOf("file:/a", "file:/b"), readings.map { it.segment.uri })
        assertEquals(4_000, readings.first().offsetMs)
        assertEquals(4_000, readings.first().bookStartMs)
        assertTrue(source.readings(25_000).isEmpty())
        assertFalse(source.isOnDevice(25_000))
        assertTrue(source.isOnDevice(-5))
    }

    @Test
    fun `librivox labels become language codes`() {
        assertEquals("en", ReadAlongLanguage.languageCode("English"))
        assertEquals("pt", ReadAlongLanguage.languageCode("por"))
        assertEquals("de", ReadAlongLanguage.languageCode("ger"))
        assertEquals("fr", ReadAlongLanguage.languageCode(" fr "))
        assertNull(ReadAlongLanguage.languageCode("Multilingual"))
        assertNull(ReadAlongLanguage.languageCode(""))
    }

    @Test
    fun `a bare code takes the device region, then an installed model, then the likely region`() {
        val supported = listOf("en-GB", "en-US", "pt-BR", "pt-PT")

        assertEquals("pt-PT", ReadAlongLanguage.locale("pt", supported, preferredRegion = "PT"))
        assertEquals("en-GB", ReadAlongLanguage.locale("en", supported, installed = listOf("en-GB"), preferredRegion = null))
        assertEquals("en-US", ReadAlongLanguage.locale("en", supported, preferredRegion = null))
        assertNull(ReadAlongLanguage.locale("ja", supported, preferredRegion = null))
    }

    @Test
    fun `detection candidates are installed only, app language first, english last`() {
        val candidates = ReadAlongLanguage.candidates(
            appLanguage = "pt-BR",
            preferred = listOf("es-ES", "pt-BR"),
            installed = listOf("en-US", "es-ES", "pt-BR"),
        )

        assertEquals(listOf("pt-BR", "es-ES", "en-US"), candidates)
        assertEquals(emptyList<String>(), ReadAlongLanguage.candidates("pt-BR", listOf("pt-BR"), installed = emptyList()))
    }

    @Test
    fun `the most confident language wins only above the threshold`() {
        assertEquals("en-US", ReadAlongLanguage.best(mapOf("en-US" to 0.8, "pt-BR" to 0.6)))
        assertEquals("en-US", ReadAlongLanguage.best(mapOf("pt-BR" to 0.7, "en-US" to 0.7)))
        assertNull(ReadAlongLanguage.best(mapOf("en-US" to 0.3)))
    }

    @Test
    fun `languages are named alone, capitalised, in the app's language`() {
        assertEquals("Inglês", ReadAlongLanguage.displayName("en-US", Locale.forLanguageTag("pt-BR")))
        assertEquals("English", ReadAlongLanguage.displayName("en_US", Locale.ENGLISH))
    }
}
