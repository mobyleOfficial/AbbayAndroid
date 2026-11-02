package com.readalong

import java.text.Normalizer
import java.util.Locale

/**
 * A book's own text (from an EPUB beside its audio), as the words a reader sees, in reading order. Read-along shows
 * these instead of the recognised words wherever it can time them against the audio ([ReadAlongAligner]).
 */
class ReadAlongBookText(
    /** The language the book declares ("en", "pt-BR"), or null. */
    val language: String?,
    /** Each word as printed, punctuation attached ("Well,", "“No.”"). */
    val words: List<String>,
    /** Indexes into [words] where a paragraph (or a chapter) starts: a line never runs across one. */
    val paragraphStarts: Set<Int>,
) {
    /** Each word as speech recognition spells it: lowercase letters and digits, no accents; empty for a lone mark. */
    val keys: List<String> by lazy { words.map(::keyOf) }

    companion object {
        /** The text of [paragraphs], in order; blank ones are dropped. */
        fun of(language: String?, paragraphs: List<String>): ReadAlongBookText {
            val words = ArrayList<String>()
            val starts = HashSet<Int>()
            for (paragraph in paragraphs) {
                val paragraphWords = paragraph.split(WORD_SEPARATORS).filter { it.isNotBlank() }
                if (paragraphWords.isEmpty()) continue
                starts += words.size
                words += paragraphWords
            }
            return ReadAlongBookText(language, words, starts)
        }

        /** How a word is compared with a recognised one: "Café," and "cafe" match, as do "couldn't" and "couldnt". */
        fun keyOf(word: String): String {
            val decomposed = Normalizer.normalize(word.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            return decomposed.filter { it.isLetterOrDigit() }
        }

        /** Spaces (non-breaking ones too), and after a dash joining two words ("then—nothing"), which stays with the first. */
        private val WORD_SEPARATORS = Regex("[\\s\\u00A0\\u2007\\u202F]+|(?<=[\\u2014\\u2013])")
    }
}
