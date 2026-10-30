package com.mobyle.abbay.data.librivox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArchiveSearchQueryTest {
    @Test
    fun everyTermIsAPrefixInTheTitleOrTheCreator() {
        assertEquals(
            "${ArchiveSearchQuery.CATALOG} AND (title:alice* OR creator:alice*) AND (title:carroll* OR creator:carroll*)",
            ArchiveSearchQuery.search("  Alice   Carroll "),
        )
    }

    @Test
    fun operatorsAreLowercasedAndLuceneCharactersEscaped() {
        assertEquals(listOf("and", "a\\:b", "c\\*"), ArchiveSearchQuery.terms("AND a:b c*"))
        assertEquals(listOf("tom", "jerry"), ArchiveSearchQuery.terms("tom+jerry"))
    }

    @Test
    fun blankTextSearchesNothing() {
        assertNull(ArchiveSearchQuery.search("   "))
        assertEquals("time machine", ArchiveSearchQuery.normalized(" Time  MACHINE"))
    }
}
