package com.readalong

import kotlin.math.max

/**
 * Matches two short runs of words one by one, in order: the most pairs of the same or nearly the same word (a longest
 * common subsequence). "Nearly" lets a misheard name ("Hermione" heard as "hermoine") still pin its place.
 */
internal object WordMatcher {
    /** Pairs of (index into [heard], index into [book]), rising in both. */
    fun matches(heard: List<String>, book: List<String>): List<Pair<Int, Int>> {
        val rows = heard.size
        val columns = book.size
        val best = Array(rows + 1) { IntArray(columns + 1) }
        for (row in rows - 1 downTo 0) {
            for (column in columns - 1 downTo 0) {
                best[row][column] = if (isSame(heard[row], book[column])) {
                    best[row + 1][column + 1] + 1
                } else {
                    max(best[row + 1][column], best[row][column + 1])
                }
            }
        }
        val pairs = ArrayList<Pair<Int, Int>>()
        var row = 0
        var column = 0
        while (row < rows && column < columns) {
            when {
                isSame(heard[row], book[column]) && best[row][column] == best[row + 1][column + 1] + 1 -> {
                    pairs += row to column
                    row += 1
                    column += 1
                }
                best[row + 1][column] >= best[row][column + 1] -> row += 1
                else -> column += 1
            }
        }
        return pairs
    }

    /** The same word, or one long enough that a slip of a letter or two still reads as it. */
    fun isSame(heard: String, book: String): Boolean {
        if (heard == book) return true
        val longer = max(heard.length, book.length)
        if (minOf(heard.length, book.length) < MIN_FUZZY_LENGTH) return false
        return distance(heard, book) * FUZZY_DENOMINATOR <= longer * FUZZY_NUMERATOR
    }

    /** Edits (insert, delete, replace a letter) from one word to the other. */
    private fun distance(first: String, second: String): Int {
        var previous = IntArray(second.length + 1) { it }
        var current = IntArray(second.length + 1)
        for (row in 1..first.length) {
            current[0] = row
            for (column in 1..second.length) {
                val replace = previous[column - 1] + if (first[row - 1] == second[column - 1]) 0 else 1
                current[column] = minOf(replace, previous[column] + 1, current[column - 1] + 1)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[second.length]
    }

    private const val MIN_FUZZY_LENGTH = 4

    /** At most 3 edits in 10 letters. */
    private const val FUZZY_NUMERATOR = 3
    private const val FUZZY_DENOMINATOR = 10
}
