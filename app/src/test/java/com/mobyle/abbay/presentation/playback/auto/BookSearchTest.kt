package com.mobyle.abbay.presentation.playback.auto

import com.model.BookFile
import com.model.BookType
import org.junit.Assert.assertEquals
import org.junit.Test

class BookSearchTest {
    private val sea = book("1", "Stories of the Sea", "Ana Ribeiro", lastPlayedAt = 5L)
    private val road = book("2", "The Long Road Home", "Test Author", lastPlayedAt = 9L)
    private val coracao = book("3", "Coração de Pedra", "João Mendes", lastPlayedAt = 1L)
    private val books = listOf(sea, road, coracao)

    @Test
    fun titleWordsWinOverAuthorWords() {
        assertEquals(listOf(road), BookSearch.search(books, "long road"))
        assertEquals(listOf(sea), BookSearch.search(books, "ribeiro"))
    }

    @Test
    fun accentsCaseAndOneLetterSlipsAreIgnored() {
        assertEquals(listOf(coracao), BookSearch.search(books, "CORACAO"))
        assertEquals(listOf(coracao), BookSearch.search(books, "joao mendez"))
        assertEquals(sea, BookSearch.search(books, "storys of the sea").first())
    }

    @Test
    fun tiesGoToTheMostRecentlyPlayed() {
        assertEquals(listOf(road, sea), BookSearch.search(books, "the"))
        assertEquals(emptyList<Any>(), BookSearch.search(books, ""))
    }

    private fun book(id: String, name: String, author: String, lastPlayedAt: Long) = BookFile(
        id = id, name = name, thumbnail = null, progress = 0, duration = 1, speed = 1f, type = BookType.FOLDER,
        fileName = "$id.mp3", author = author, lastPlayedAt = lastPlayedAt,
    )
}
