package com.usecase

import com.library.NaturalOrderComparator
import com.model.Book
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

class SearchBooks @Inject constructor() {
    private val naturalOrder = NaturalOrderComparator()

    operator fun invoke(books: List<Book>, query: String): List<Book> {
        val terms = wordsOf(query)
        if (terms.isEmpty()) return emptyList()
        return books.mapNotNull { book ->
            val titleWords = wordsOf(book.name)
            val otherWords = wordsOf(book.author.orEmpty()) + wordsOf(book.narrator.orEmpty())
            val allWords = titleWords + otherWords
            if (!terms.all { term -> allWords.any { it.startsWith(term) } }) return@mapNotNull null
            val isTitleMatch = terms.all { term -> titleWords.any { it.startsWith(term) } }
            book to isTitleMatch
        }.sortedWith(compareByDescending<Pair<Book, Boolean>> { it.second }.thenBy(naturalOrder) { it.first.name })
            .map { it.first }
    }

    private fun wordsOf(text: String): List<String> {
        val folded = Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase(Locale.ROOT)
        return folded.split(SEPARATORS).filter(String::isNotEmpty)
    }

    private companion object {
        val DIACRITICS = Regex("\\p{M}+")
        val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    }
}
