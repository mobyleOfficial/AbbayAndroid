package com.mobyle.abbay.presentation.booklist

import android.app.Application
import com.mobyle.abbay.fakes.FakeLibraryRepository
import com.mobyle.abbay.fakes.FakeSettingsRepository
import com.mobyle.abbay.fakes.SampleBooks
import com.mobyle.abbay.fakes.ViewModelFactory
import com.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class BooksListViewModelWalkthroughTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a new listener sees the tour, and finishing it keeps it away`() {
        val factory = ViewModelFactory()
        val viewModel = factory.booksListViewModel()

        assertTrue(viewModel.showWalkthrough.value)

        viewModel.dismissWalkthrough()
        assertFalse(viewModel.showWalkthrough.value)
        assertTrue(factory.settingsRepository.settings.value.walkthroughShown)
    }

    @Test
    fun `someone who already has books never gets the tour uninvited`() {
        val factory = ViewModelFactory(libraryRepository = FakeLibraryRepository(listOf(SampleBooks.singleFileBook)))
        val viewModel = factory.booksListViewModel()

        assertFalse(viewModel.showWalkthrough.value)
        assertTrue(factory.settingsRepository.settings.value.walkthroughShown)
    }

    @Test
    fun `replaying from Settings brings the tour back`() {
        val factory = ViewModelFactory(settingsRepository = FakeSettingsRepository(AppSettings(walkthroughShown = true)))
        val viewModel = factory.booksListViewModel()
        assertFalse(viewModel.showWalkthrough.value)

        factory.settingsRepository.settings.value = factory.settingsRepository.settings.value.copy(walkthroughShown = false)
        assertTrue(viewModel.showWalkthrough.value)
    }
}
