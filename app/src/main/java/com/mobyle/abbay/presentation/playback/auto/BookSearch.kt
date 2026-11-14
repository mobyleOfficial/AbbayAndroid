package com.mobyle.abbay.presentation.playback.auto

import com.model.Book
import java.text.Normalizer

object BookSearch {
    fun search(books: List<Book>, query: String): List<Book> {
        val queryWords = words(query)
        if (queryWords.isEmpty()) return emptyList()
        return books.asSequence()
            .map { book -> book to score(book, queryWords) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<Book, Int>> { it.second }.thenByDescending { it.first.lastPlayedAt ?: 0L })
            .map { it.first }
            .toList()
    }

    private fun score(book: Book, queryWords: List<String>): Int {
        val titleWords = words(book.name)
        val authorWords = words(book.author.orEmpty())
        return queryWords.sumOf { word ->
            when {
                titleWords.any { it == word } -> EXACT_TITLE
                titleWords.any { it.startsWith(word) || isOneEditAway(it, word) } -> NEAR_TITLE
                authorWords.any { it == word || it.startsWith(word) || isOneEditAway(it, word) } -> AUTHOR
                else -> 0
            }.toInt()
        }
    }

    private fun words(text: String): List<String> = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .split(NON_LETTERS)
        .filter { it.length > 1 }

    private fun isOneEditAway(first: String, second: String): Boolean {
        if (minOf(first.length, second.length) < MINIMUM_FUZZY_LENGTH || kotlin.math.abs(first.length - second.length) > 1) return false
        var firstIndex = 0
        var secondIndex = 0
        var edits = 0
        while (firstIndex < first.length && secondIndex < second.length) {
            if (first[firstIndex] == second[secondIndex]) {
                firstIndex++
                secondIndex++
                continue
            }
            if (++edits > 1) return false
            when {
                first.length > second.length -> firstIndex++
                first.length < second.length -> secondIndex++
                else -> {
                    firstIndex++
                    secondIndex++
                }
            }
        }
        return edits + (first.length - firstIndex) + (second.length - secondIndex) <= 1
    }

    private const val EXACT_TITLE = 3
    private const val NEAR_TITLE = 2
    private const val AUTHOR = 1
    private const val MINIMUM_FUZZY_LENGTH = 4
    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_LETTERS = Regex("[^\\p{L}\\p{N}]+")
}
