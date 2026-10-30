package com.mobyle.abbay.presentation.booklist

import android.app.Application
import com.library.LibrarySortOrder
import com.library.LibraryTab
import com.mobyle.abbay.fakes.FakeLegacyMigrationRepository
import com.mobyle.abbay.fakes.FakeLibraryRepository
import com.mobyle.abbay.fakes.FakeSettingsRepository
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.fakes.ViewModelFactory
import com.mobyle.abbay.presentation.booklist.BooksListViewModel.BooksListUiState
import com.model.BookState
import com.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Library flows of the book list (Phase 1: LIB-01). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class BooksListViewModelLibraryTest {
    private val inProgressBook = SampleBooks.multipleFilesBook.copy(state = BookState.IN_PROGRESS, lastPlayedAt = 20L)
    private val newBook = SampleBooks.singleFileBook

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun factory(
        settings: AppSettings = AppSettings(),
        legacy: FakeLegacyMigrationRepository = FakeLegacyMigrationRepository(),
    ) = ViewModelFactory(
        libraryRepository = FakeLibraryRepository(listOf(newBook, inProgressBook)),
        settingsRepository = FakeSettingsRepository(settings),
        legacyRepository = legacy,
    )

    @Test
    fun `startup validates roots, runs a quick scan and imports legacy progress`() {
        val factory = factory()
        factory.booksListViewModel()

        assertEquals(listOf("validateRoots", "quickScan"), factory.libraryRepository.calls)
        assertEquals(1, factory.legacyRepository.importCalls)
    }

    @Test
    fun `default tab is In progress when a book is in progress`() {
        val viewModel = factory().booksListViewModel()

        assertEquals(LibraryTab.IN_PROGRESS, viewModel.libraryState.value.tab)
        assertEquals(listOf(inProgressBook.id), (viewModel.uiState.value as BooksListUiState.BookListSuccess).audiobookList.map { it.id })
    }

    @Test
    fun `selecting a tab filters the list`() {
        val viewModel = factory().booksListViewModel()

        viewModel.selectTab(LibraryTab.ALL)

        assertEquals(2, (viewModel.uiState.value as BooksListUiState.BookListSuccess).audiobookList.size)
    }

    @Test
    fun `picking the current sort order flips the direction and a new order uses its default direction`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()

        viewModel.selectSortOrder(LibrarySortOrder.RECENTLY_PLAYED)
        assertTrue(factory.settingsRepository.settings.value.librarySortAscending)

        viewModel.selectSortOrder(LibrarySortOrder.TITLE)
        assertEquals(LibrarySortOrder.TITLE, factory.settingsRepository.settings.value.librarySortOrder)
        assertTrue(factory.settingsRepository.settings.value.librarySortAscending)
    }

    @Test
    fun `an accessible legacy folder becomes a root without asking`() {
        val factory = factory(legacy = FakeLegacyMigrationRepository("content://legacy/tree", isStillAccessible = true))
        val viewModel = factory.booksListViewModel()

        assertTrue(factory.libraryRepository.calls.contains("addRoot(content://legacy/tree)"))
        assertTrue(factory.libraryRepository.calls.contains("requestFullScan"))
        assertTrue(factory.legacyRepository.migrated)
        assertFalse(viewModel.uiState.value is BooksListUiState.LegacyFolderMigration)
    }

    @Test
    fun `a legacy folder without permission shows the one-time migration screen`() {
        val factory = factory(legacy = FakeLegacyMigrationRepository("content://legacy/tree", isStillAccessible = false))
        val viewModel = factory.booksListViewModel()

        assertEquals(BooksListUiState.LegacyFolderMigration("content://legacy/tree"), viewModel.uiState.value)

        viewModel.completeLegacyMigration("content://picked/tree")

        assertTrue(factory.libraryRepository.calls.contains("addRoot(content://picked/tree)"))
        assertTrue(factory.legacyRepository.migrated)
        assertTrue(viewModel.uiState.value is BooksListUiState.BookListSuccess)
    }

    @Test
    fun `refresh explains itself the first time, then rescans`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()

        viewModel.onRefreshClicked()
        assertTrue(viewModel.showReloadGuide.value)
        assertFalse(factory.libraryRepository.calls.contains("requestFullScan"))

        viewModel.dismissReloadGuide()
        viewModel.onRefreshClicked()
        assertTrue(factory.libraryRepository.calls.contains("requestFullScan"))
    }

    @Test
    fun `the last played book is restored and its progress is saved with track and speed`() {
        val factory = factory(settings = AppSettings(lastBookId = inProgressBook.id))
        val viewModel = factory.booksListViewModel()

        assertEquals(inProgressBook.id, viewModel.selectedBook.value?.id)

        viewModel.persistProgress()

        assertTrue(factory.libraryRepository.calls.contains("saveListeningState(201, 1, 120000, 1.5)"))
    }

    @Test
    fun `removing a book hides it and marking a book changes its state`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()

        viewModel.removeBook(newBook)
        viewModel.markBook(inProgressBook, BookState.FINISHED)

        assertTrue(factory.libraryRepository.calls.contains("hideBook(101)"))
        assertTrue(factory.libraryRepository.calls.contains("setBookState(201, FINISHED)"))
    }

    @Test
    fun `an empty library shows the empty state`() {
        val factory = ViewModelFactory()
        val viewModel = factory.booksListViewModel()

        assertEquals(BooksListUiState.NoBookSelected, viewModel.uiState.value)
    }

    @Test
    fun `a last book set later by the legacy import is restored`() {
        val factory = factory()
        val viewModel = factory.booksListViewModel()
        assertEquals(null, viewModel.selectedBook.value)

        factory.settingsRepository.settings.value = factory.settingsRepository.settings.value.copy(lastBookId = inProgressBook.id)

        assertEquals(inProgressBook.id, viewModel.selectedBook.value?.id)
    }
}
