package com.readalong

/** A heard word matched to a book word: [heard] into the heard words, [book] into the book's words with content. */
internal data class Match(val heard: Int, val book: Int)

/**
 * Where heard words and the book's run together. [longestChain] keeps the anchors that follow one another in both;
 * [of] cuts the matches into passages where the two part ways, and keeps the ones to trust.
 */
internal object ReadAlongPassages {
    /**
     * Matches a passage needs to be shown: fewer is likelier a title or a stock phrase read out (a narrator saying
     * the book's title matches its title page) than the story. A page of the story has hundreds.
     */
    const val MIN_PASSAGE_MATCHES = 20

    /** Matches a short passage needs when it is read straight between two others. */
    const val MIN_LINKED_MATCHES = 4

    /** The most heard words between two passages read one straight after the other. */
    private const val MAX_LINK_GAP = 10

    /** Between two matches, one side skipping more than this many times the other's words, plus [GAP_SLACK], parts them. */
    private const val GAP_RATIO = 3
    private const val GAP_SLACK = 20

    /** The trusted passages in [pairs] (rising in both heard and book order). */
    fun of(pairs: List<Match>): List<List<Match>> = trusted(passages(pairs))

    /** The longest run of [anchors] (in heard order) whose book positions rise too: chance matches fall out of it. */
    fun longestChain(anchors: List<Match>): List<Match> {
        if (anchors.isEmpty()) return emptyList()
        val tails = ArrayList<Int>()
        val previous = IntArray(anchors.size) { -1 }
        for ((index, anchor) in anchors.withIndex()) {
            var low = 0
            var high = tails.size
            while (low < high) {
                val middle = (low + high) ushr 1
                if (anchors[tails[middle]].book < anchor.book) low = middle + 1 else high = middle
            }
            if (low > 0) previous[index] = tails[low - 1]
            if (low == tails.size) tails += index else tails[low] = index
        }
        val chain = ArrayList<Match>()
        var cursor = tails.last()
        while (cursor >= 0) {
            chain += anchors[cursor]
            cursor = previous[cursor]
        }
        return chain.asReversed()
    }

    /** [pairs] cut where book and audio part ways: one skips well more than the other between two matches. */
    private fun passages(pairs: List<Match>): List<List<Match>> {
        val passages = ArrayList<List<Match>>()
        var current = ArrayList<Match>()
        for (pair in pairs) {
            val last = current.lastOrNull()
            if (last != null && isBreak(pair.heard - last.heard - 1, pair.book - last.book - 1)) {
                passages += current
                current = ArrayList()
            }
            current += pair
        }
        if (current.isNotEmpty()) passages += current
        return passages
    }

    /**
     * The passages to show: long ones, and short ones read straight between two others (a verse between notes the
     * narrator skips, as in an annotated edition). A short one at either end of what was heard (a title read out before
     * the story) is likelier chance, and stays as heard.
     */
    private fun trusted(passages: List<List<Match>>): List<List<Match>> = passages.filterIndexed { index, passage ->
        val before = passages.getOrNull(index - 1)
        val after = passages.getOrNull(index + 1)
        passage.size >= MIN_PASSAGE_MATCHES ||
            (passage.size >= MIN_LINKED_MATCHES && before != null && after != null && follows(before, passage) && follows(passage, after))
    }

    /** [next] is heard right after [previous]: at most [MAX_LINK_GAP] heard words apart. */
    private fun follows(previous: List<Match>, next: List<Match>): Boolean = next.first().heard - previous.last().heard - 1 <= MAX_LINK_GAP

    private fun isBreak(heardGap: Int, bookGap: Int): Boolean =
        bookGap > heardGap * GAP_RATIO + GAP_SLACK || heardGap > bookGap * GAP_RATIO + GAP_SLACK
}
