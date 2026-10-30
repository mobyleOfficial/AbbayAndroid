package com.usecase

import com.model.BookFile
import com.model.BookType
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchBooksTest {
    private val books = listOf(
        book("1", "O Cortiço", author = "Aluísio Azevedo"),
        book("2", "Memórias Póstumas", author = "Machado de Assis", narrator = "Ana Cortez"),
        book("3", "Dom Casmurro", author = "Machado de Assis"),
    )
    private val search = SearchBooks()

    @Test
    fun matchesWordStartsWithoutCaseOrAccents() {
        assertEquals(listOf("1"), search(books, "cortico").map { it.id })
        assertEquals(listOf("3", "2"), search(books, "MACH").map { it.id })
        assertEquals(listOf("3"), search(books, "machado dom").map { it.id })
        assertEquals(emptyList<String>(), search(books, "  ").map { it.id })
    }

    @Test
    fun titleMatchesComeBeforeAuthorAndNarratorMatches() {
        // "Cort" starts a word of title 1 and of the narrator of 2.
        assertEquals(listOf("1", "2"), search(books, "cort").map { it.id })
    }

    private fun book(id: String, title: String, author: String? = null, narrator: String? = null) = BookFile(
        id = id,
        name = title,
        thumbnail = null,
        progress = 0,
        duration = 1,
        speed = 1f,
        type = BookType.FILE,
        fileName = "$id.mp3",
        author = author,
        narrator = narrator,
    )
}
