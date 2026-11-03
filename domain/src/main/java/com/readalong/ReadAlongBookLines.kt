package com.readalong

/**
 * Cuts the book's timed words into lines a reader can follow, the way the text itself phrases them: a line ends with
 * a sentence, never runs into a new paragraph, ends at a comma once it is long, and in any case at [MAX_WORDS], at its
 * last comma past the middle when it has one.
 */
internal object ReadAlongBookLines {
    const val MAX_WORDS = 12

    /** Words past this many end a line at a comma. */
    private const val COMMA_BREAK_WORDS = 7

    /** [words] are the book's from index [firstIndex] on; [paragraphStarts] are book indexes too. */
    fun lines(words: List<ReadAlongWord>, firstIndex: Int, paragraphStarts: Set<Int>): List<ReadAlongLine> {
        val lines = ArrayList<ReadAlongLine>()
        var current = ArrayList<ReadAlongWord>()
        for ((offset, word) in words.withIndex()) {
            if (current.isNotEmpty() && (firstIndex + offset) in paragraphStarts) {
                lines += lineOf(current)
                current = ArrayList()
            }
            current += word
            when {
                endsSentence(word.text) || (current.size >= COMMA_BREAK_WORDS && endsClause(word.text)) -> {
                    lines += lineOf(current)
                    current = ArrayList()
                }
                current.size >= MAX_WORDS -> {
                    val cut = clauseCut(current)
                    lines += lineOf(current.subList(0, cut))
                    current = ArrayList(current.subList(cut, current.size))
                }
            }
        }
        if (current.isNotEmpty()) lines += lineOf(current)
        return lines
    }

    /** After the last word ending a clause in the line's second half, else the whole line. */
    private fun clauseCut(words: List<ReadAlongWord>): Int {
        val last = (words.size / 2 until words.size - 1).lastOrNull { endsClause(words[it].text) }
        return if (last == null) words.size else last + 1
    }

    /** A sentence ends at its mark, but not at a title's ("D. Evarista", "Dr. Simão", "Mr. Smith"). */
    private fun endsSentence(word: String): Boolean =
        word.trimEnd { it in CLOSING }.lastOrNull() in SENTENCE_ENDS && !ABBREVIATION.matches(word)

    private fun endsClause(word: String): Boolean = word.trimEnd { it in CLOSING }.lastOrNull() in CLAUSE_ENDS

    private fun lineOf(words: List<ReadAlongWord>): ReadAlongLine =
        ReadAlongLine(words.first().startMs, words.last().endMs, words.toList(), fromBook = true)

    private val SENTENCE_ENDS = setOf('.', '!', '?', '…')
    private val CLAUSE_ENDS = setOf(',', ';', ':', '—', '–')

    /** A capital and at most two more letters before a full stop: a title's abbreviation. */
    private val ABBREVIATION = Regex("\\p{Lu}\\p{Ll}{0,2}\\.")

    /** Quotes and brackets closing after the mark: "No.” ends a sentence. */
    private val CLOSING = setOf('"', '\'', '”', '’', '»', ')', ']', '*')
}
