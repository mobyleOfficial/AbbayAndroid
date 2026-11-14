package com.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTabTest {
    @Test
    fun `ordered keeps the chosen order and appends tabs missing from it`() {
        assertEquals(
            listOf(LibraryTab.ALL, LibraryTab.NEW, LibraryTab.IN_PROGRESS, LibraryTab.FINISHED),
            LibraryTab.ordered(listOf(LibraryTab.ALL, LibraryTab.NEW)),
        )
    }

    @Test
    fun `visible leaves hidden tabs out`() {
        assertEquals(
            listOf(LibraryTab.FINISHED, LibraryTab.ALL),
            LibraryTab.visible(
                listOf(LibraryTab.FINISHED, LibraryTab.IN_PROGRESS, LibraryTab.NEW, LibraryTab.ALL),
                setOf(LibraryTab.IN_PROGRESS, LibraryTab.NEW),
            ),
        )
    }

    @Test
    fun `visible keeps All when every tab is hidden`() {
        assertEquals(listOf(LibraryTab.ALL), LibraryTab.visible(LibraryTab.entries, LibraryTab.entries.toSet()))
    }
}
