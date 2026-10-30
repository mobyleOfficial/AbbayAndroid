package com.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NaturalOrderComparatorTest {
    private val comparator = NaturalOrderComparator(Locale.ENGLISH)

    private fun sorted(vararg names: String) = names.toList().shuffled(java.util.Random(7)).sortedWith(comparator)

    @Test
    fun `spec example order`() {
        assertEquals(
            listOf("Chapter 1", "Chapter 2", "Chapter 10", "Chapter 10a"),
            sorted("Chapter 10a", "Chapter 2", "Chapter 10", "Chapter 1"),
        )
    }

    @Test
    fun `numbers compare by value, not by text`() {
        assertEquals(listOf("2", "9", "10", "100"), sorted("100", "10", "9", "2"))
    }

    @Test
    fun `leading zeros do not change numeric order`() {
        assertEquals(listOf("Track 001", "Track 2", "Track 0010"), sorted("Track 0010", "Track 2", "Track 001"))
    }

    @Test
    fun `numbers longer than a long still compare`() {
        assertEquals(
            listOf("part 99999999999999999999", "part 100000000000000000000"),
            sorted("part 100000000000000000000", "part 99999999999999999999"),
        )
    }

    @Test
    fun `case and accents are ignored at primary strength`() {
        assertTrue(comparator.compare("capitulo 1", "Capítulo 2") < 0)
        assertTrue(comparator.compare("Capítulo 2", "capitulo 10") < 0)
    }

    @Test
    fun `underscore, dash and dot behave like spaces`() {
        assertEquals(
            listOf("Book_01", "Book-2", "Book.3", "Book 10"),
            sorted("Book 10", "Book.3", "Book_01", "Book-2"),
        )
    }

    @Test
    fun `fewer runs come first when all compared runs are equal`() {
        assertTrue(comparator.compare("Chapter 10", "Chapter 10a") < 0)
    }

    @Test
    fun `empty names sort first and do not crash`() {
        assertEquals(listOf("", "a"), sorted("a", ""))
    }

    @Test
    fun `comparison is consistent with equality for identical names`() {
        assertEquals(0, comparator.compare("Disc 1", "Disc 1"))
    }
}
