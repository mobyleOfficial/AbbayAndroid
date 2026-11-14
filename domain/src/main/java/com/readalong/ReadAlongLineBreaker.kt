package com.readalong

/**
 * Cuts the words of one recognised utterance into lines a reader can follow. An utterance runs until the narrator
 * pauses long enough, which can be twenty seconds of speech: shown whole it would fill the square. A line ends at a
 * breath (a gap of [BREATH_MS] or more) once it has [MIN_WORDS], and in any case at [MAX_WORDS], where it breaks at its
 * widest gap among the last words rather than mid-phrase when it can.
 */
object ReadAlongLineBreaker {
    const val BREATH_MS = 300L
    const val MIN_WORDS = 4
    const val MAX_WORDS = 12

    fun lines(words: List<ReadAlongWord>): List<ReadAlongLine> {
        val lines = ArrayList<ReadAlongLine>()
        var current = ArrayList<ReadAlongWord>()
        for (word in words) {
            val previous = current.lastOrNull()
            if (previous != null && current.size >= MIN_WORDS && word.startMs - previous.endMs >= BREATH_MS) {
                lines += lineOf(current)
                current = ArrayList()
            }
            current += word
            if (current.size >= MAX_WORDS) {
                val cut = widestGapCut(current)
                lines += lineOf(current.subList(0, cut))
                current = ArrayList(current.subList(cut, current.size))
            }
        }
        if (current.isNotEmpty()) lines += lineOf(current)
        return lines
    }

    /** Where to cut a full line: after the word followed by the widest gap in its second half, so the rest starts a phrase. */
    private fun widestGapCut(words: List<ReadAlongWord>): Int {
        var best = words.size
        var widest = -1L
        for (index in words.size / 2 until words.size - 1) {
            val gap = words[index + 1].startMs - words[index].endMs
            if (gap > widest) {
                widest = gap
                best = index + 1
            }
        }
        return best
    }

    private fun lineOf(words: List<ReadAlongWord>): ReadAlongLine = ReadAlongLine(words.first().startMs, words.last().endMs, words.toList())
}
