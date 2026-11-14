package com.readalong

/**
 * Times a book's own text against what speech recognition heard, so read-along can show the real text, punctuated
 * and spelled right, where the audio reads it.
 *
 * Recognition gets most words right but not all (names above all), so the two are matched in two steps. Anchors
 * first: runs of [ANCHOR_WORDS] heard words that appear exactly once in the book, kept only when they follow one
 * another in both (a longest chain), which drops chance matches. Then the words between two anchors are matched one by
 * one. A stretch of matches is a passage: its book words are shown, the matched ones at the time they were heard and
 * the rest spread between them. A passage ends where book and audio part ways (an intro the book doesn't have, a
 * skipped page), and there the recognised lines are shown instead, as they are where nothing matched; a recognised
 * line reaching into a passage gives way to it, so nothing is shown twice.
 *
 * Pure and immutable; the book's index is built once per text, and each transcript is aligned from scratch, which
 * keeps it right however the transcript grew (runs arrive in any order).
 */
class ReadAlongAligner(private val text: ReadAlongBookText) {
    /** Indexes into [ReadAlongBookText.words] of the words with something to match: lone marks ("—") have nothing. */
    private val content: IntArray = text.keys.indices.filter { text.keys[it].isNotEmpty() }.toIntArray()

    /**
     * Each run of [ANCHOR_WORDS] content words found exactly once in the book, to where it starts in [content]. Built
     * with the aligner, so making one off the main thread leaves nothing heavy for the first transcript.
     */
    private val anchorIndex: Map<String, Int> = HashMap<String, Int>(content.size).also { index ->
        for (start in 0..content.size - ANCHOR_WORDS) {
            val key = (start until start + ANCHOR_WORDS).joinToString(" ") { text.keys[content[it]] }
            index[key] = if (index.containsKey(key)) DUPLICATE else start
        }
    }

    /** [transcript] with the book's text in place of the recognised lines wherever the two match. */
    fun aligned(transcript: ReadAlongTranscript): ReadAlongTranscript {
        val heard = transcript.lines.flatMap { line -> line.words }.filter { ReadAlongBookText.keyOf(it.text).isNotEmpty() }
        val heardKeys = heard.map { ReadAlongBookText.keyOf(it.text) }
        val passages = if (heard.size < ANCHOR_WORDS || content.size < ANCHOR_WORDS) {
            emptyList()
        } else {
            ReadAlongPassages.of(filledPairs(chainedPairs(heardKeys), heardKeys))
        }
        return if (passages.isEmpty()) transcript else withBookLines(transcript, heard, passages)
    }

    /** The passages' book lines, and the recognised lines outside them. */
    private fun withBookLines(
        transcript: ReadAlongTranscript,
        heard: List<ReadAlongWord>,
        passages: List<List<Match>>,
    ): ReadAlongTranscript {
        val bookLines = passages.flatMap { passage -> bookLines(passage, heard) }
        val spans = passages.map { passage -> heard[passage.first().heard].startMs..heard[passage.last().heard].endMs }
        val recognised = transcript.lines.filter { line -> spans.none { line.startMs <= it.last && line.endMs >= it.first } }
        return ReadAlongTranscript(transcript.language, (bookLines + recognised).sortedBy { it.startMs }, transcript.covered)
    }

    /** The anchors that follow one another in both orders, each spelled out as its words' matches. */
    private fun chainedPairs(heardKeys: List<String>): List<Match> {
        val anchors = ArrayList<Match>()
        for (start in 0..heardKeys.size - ANCHOR_WORDS) {
            val key = heardKeys.subList(start, start + ANCHOR_WORDS).joinToString(" ")
            val book = anchorIndex[key] ?: continue
            if (book != DUPLICATE) anchors += Match(start, book)
        }
        val pairs = ArrayList<Match>()
        for (anchor in ReadAlongPassages.longestChain(anchors)) {
            for (offset in 0 until ANCHOR_WORDS) {
                val match = Match(anchor.heard + offset, anchor.book + offset)
                val last = pairs.lastOrNull()
                if (last == null || (match.heard > last.heard && match.book > last.book)) pairs += match
            }
        }
        return pairs
    }

    /** [pairs] with the words between each two matched one by one, where the gap is short enough to be the same passage. */
    private fun filledPairs(pairs: List<Match>, heardKeys: List<String>): List<Match> {
        val filled = ArrayList<Match>(pairs.size * 2)
        for ((index, pair) in pairs.withIndex()) {
            filled += pair
            val next = pairs.getOrNull(index + 1) ?: continue
            val heardGap = next.heard - pair.heard - 1
            val bookGap = next.book - pair.book - 1
            if (heardGap in 1..MAX_FILLED_GAP && bookGap in 1..MAX_FILLED_GAP) {
                filled += WordMatcher.matches(
                    heard = heardKeys.subList(pair.heard + 1, next.heard),
                    book = (pair.book + 1 until next.book).map { text.keys[content[it]] },
                ).map { (heardOffset, bookOffset) -> Match(pair.heard + 1 + heardOffset, pair.book + 1 + bookOffset) }
            }
        }
        return filled
    }

    /** The book's words from the passage's first match to its last, timed, as lines. */
    private fun bookLines(passage: List<Match>, heard: List<ReadAlongWord>): List<ReadAlongLine> {
        val firstWord = content[passage.first().book]
        val lastWord = content[passage.last().book]
        val heardAt = passage.associate { content[it.book] to heard[it.heard] }
        val timed = ArrayList<ReadAlongWord>(lastWord - firstWord + 1)
        var wordIndex = firstWord
        while (wordIndex <= lastWord) {
            val match = heardAt[wordIndex]
            if (match != null) {
                timed += ReadAlongWord(text.words[wordIndex], match.startMs, match.endMs)
                wordIndex += 1
            } else {
                val nextMatched = (wordIndex + 1..lastWord).first { it in heardAt }
                timed += spread(wordIndex until nextMatched, timed.last().endMs, heardAt.getValue(nextMatched).startMs)
                wordIndex = nextMatched
            }
        }
        return ReadAlongBookLines.lines(timed, firstWord, text.paragraphStarts)
    }

    /** Book words nothing was heard for, shared evenly over the time between the matched words around them. */
    private fun spread(indexes: IntRange, fromMs: Long, toMs: Long): List<ReadAlongWord> {
        val count = indexes.count()
        val span = (toMs - fromMs).coerceAtLeast(0L)
        return indexes.mapIndexed { order, wordIndex ->
            val start = fromMs + span * order / count
            ReadAlongWord(text.words[wordIndex], start, fromMs + span * (order + 1) / count)
        }
    }

    companion object {
        /** Heard words in a row that must appear once in the book to anchor the two together. */
        const val ANCHOR_WORDS = 4

        /** The most words between two matches that are matched one by one. */
        private const val MAX_FILLED_GAP = 80

        private const val DUPLICATE = -1
    }
}
