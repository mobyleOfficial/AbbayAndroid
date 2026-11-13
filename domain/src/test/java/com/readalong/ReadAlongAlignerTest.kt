package com.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAlongAlignerTest {
    private val book = ReadAlongBookText.of(
        language = "en",
        paragraphs = listOf(
            "Chapter One",
            "The old man looked at the sea. It was calm, and Santiago smiled at the gulls overhead.",
            "He said, “We will go out tomorrow, before the sun is up.” Nobody answered him.",
        ),
    )

    /** Heard words, a second apart from [startMs], as one recognised line each [perLine] words. */
    private fun heard(text: String, startMs: Long = 10_000L, perLine: Int = 6): ReadAlongTranscript {
        val words = text.split(' ').mapIndexed { index, word ->
            ReadAlongWord(word, startMs + index * WORD_MS, startMs + index * WORD_MS + WORD_MS - 100)
        }
        val lines = words.chunked(perLine).map { ReadAlongLine(it.first().startMs, it.last().endMs, it) }
        return ReadAlongTranscript("en-US", lines, listOf(ReadAlongSpan(0, words.last().endMs + 1)))
    }

    private fun shownText(transcript: ReadAlongTranscript) = transcript.lines.joinToString(" / ") { it.text }

    @Test
    fun `the book's text replaces what was heard, punctuated and cut at its sentences`() {
        val transcript = heard(STORY)

        val aligned = ReadAlongAligner(book).aligned(transcript)

        assertEquals(
            "The old man looked at the sea. / It was calm, and Santiago smiled at the gulls overhead. / " +
                "He said, “We will go out tomorrow, / before the sun is up.” / Nobody answered him.",
            shownText(aligned),
        )
        assertTrue(aligned.lines.all { it.fromBook })
        assertEquals(transcript.covered, aligned.covered)
    }

    @Test
    fun `a misheard name keeps its place and the time it was heard`() {
        val transcript = heard(STORY.replace("santiago", "san diego"))

        val words = ReadAlongAligner(book).aligned(transcript).lines.flatMap { it.words }

        val santiago = words.single { it.text == "Santiago" }
        val and = words.single { it.text == "and" }
        val smiled = words.single { it.text == "smiled" }
        assertTrue(santiago.startMs >= and.endMs && santiago.endMs <= smiled.startMs)
    }

    @Test
    fun `words are timed as they were heard`() {
        val transcript = heard(STORY)

        val sea = ReadAlongAligner(book).aligned(transcript).lines.flatMap { it.words }.single { it.text == "sea." }

        val heardSea = transcript.lines.flatMap { it.words }.single { it.text == "sea" }
        assertEquals(heardSea.startMs, sea.startMs)
    }

    @Test
    fun `speech the book doesn't have stays as it was heard`() {
        val transcript = heard("this is a public domain recording read by someone $STORY")

        val aligned = ReadAlongAligner(book).aligned(transcript)

        assertFalse(aligned.lines.first().fromBook)
        assertTrue(aligned.lines.first().text.startsWith("this is a public"))
        assertTrue(aligned.lines.last().fromBook)
        assertEquals(1, aligned.lines.count { it.text.contains("old man", ignoreCase = true) })
    }

    @Test
    fun `a transcript from another book is left as it is`() {
        val transcript = heard("call me ishmael some years ago never mind how long precisely having little or no money")

        assertSame(transcript, ReadAlongAligner(book).aligned(transcript))
    }

    @Test
    fun `a title read out matches too little of the book to replace what was heard`() {
        val transcript = heard("the old man looked at the sea it was calm")

        assertSame(transcript, ReadAlongAligner(book).aligned(transcript))
    }

    @Test
    fun `a short verse read between notes the narrator skips is shown from the book`() {
        val annotated = ReadAlongBookText.of(
            language = "en",
            paragraphs = listOf(
                "1. The art of war is of vital importance to the state, a matter of life and death, a road either to safety or to ruin.",
                "[A long note the narrator doesn't read: Tu Mu and Li Chuan both say the same thing here, at great length, " +
                    "quoting older commentators, older battles and older generals, none of whom the recording ever mentions.]",
                "2. Heaven signifies night and day, cold and heat.",
                "[Another long note nobody reads aloud, about seasons, weather and the movements of the stars over the camp, " +
                    "with a digression on the calendar, the five elements and a famous siege that went badly for everyone.]",
                "3. Earth comprises distances, great and small; danger and security; open ground and narrow passes; " +
                    "the chances of life and death, and of victory or defeat in every battle.",
            ),
        )
        val transcript = heard(
            "the art of war is of vital importance to the state a matter of life and death a road either to safety or to ruin " +
                "heaven signifies night and day cold and heat earth comprises distances great and small danger and security " +
                "open ground and narrow passes the chances of life and death and of victory or defeat in every battle",
        )

        val lines = ReadAlongAligner(annotated).aligned(transcript).lines

        assertTrue(lines.all { it.fromBook })
        assertTrue(lines.any { it.text.contains("signifies night and day, cold and heat.") })
        assertFalse(lines.any { it.text.contains("note") })
    }

    @Test
    fun `a title's abbreviation doesn't end the line`() {
        val words = "D. Evarista era mal composta de feições. Dr. Bacamarte sorriu.".split(' ')
            .mapIndexed { index, word -> ReadAlongWord(word, index * WORD_MS, index * WORD_MS + WORD_MS) }

        val lines = ReadAlongBookLines.lines(words, firstIndex = 0, paragraphStarts = emptySet()).map { it.text }

        assertEquals(listOf("D. Evarista era mal composta de feições.", "Dr. Bacamarte sorriu."), lines)
    }

    @Test
    fun `book keys ignore case, accents and marks`() {
        assertEquals("cafe", ReadAlongBookText.keyOf("Café,"))
        assertEquals("couldnt", ReadAlongBookText.keyOf("couldn't"))
        assertEquals("", ReadAlongBookText.keyOf("—"))
    }

    private companion object {
        const val WORD_MS = 400L
        const val STORY = "the old man looked at the sea it was calm and santiago smiled at the gulls overhead " +
            "he said we will go out tomorrow before the sun is up nobody answered him"
    }
}
