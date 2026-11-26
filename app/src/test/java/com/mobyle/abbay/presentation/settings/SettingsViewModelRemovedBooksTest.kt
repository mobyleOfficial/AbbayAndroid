package com.mobyle.abbay.presentation.settings

import com.mobyle.abbay.fakes.FakeLibraryRepository
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.fakes.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelRemovedBooksTest {
    private val book = SampleBooks.singleFileBook
    private val otherBook = SampleBooks.multipleFilesBook

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a deleted book is listed and restoring it puts it back in the library`() = runTest {
        val library = FakeLibraryRepository(listOf(book, otherBook))
        val viewModel = ViewModelFactory(libraryRepository = library).settingsViewModel()

        library.hideBook(book.id)
        assertEquals(listOf(book.id), viewModel.removedBooks.value.map { it.id })

        viewModel.restoreRemovedBook(book.id)

        assertEquals(emptyList<String>(), viewModel.removedBooks.value.map { it.id })
        assertEquals(setOf(book.id, otherBook.id), library.books.value.map { it.id }.toSet())
    }
}
