package com.mobyle.abbay.presentation.audiobookshelf

import com.mobyle.abbay.fakes.FakeAudiobookshelfRepository
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfSignInError
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.SignInAudiobookshelf
import com.usecase.SignOutAudiobookshelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudiobookshelfSignInViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeAudiobookshelfRepository) = AudiobookshelfSignInViewModel(
        observeAccount = ObserveAudiobookshelfAccount(repository),
        signInAudiobookshelf = SignInAudiobookshelf(repository),
        signOutAudiobookshelf = SignOutAudiobookshelf(repository),
    )

    @Test
    fun `a refused sign in keeps the form with its reason`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAudiobookshelfRepository(signInError = AudiobookshelfSignInError.WRONG_CREDENTIALS))

        viewModel.signIn("abs.example.com", "abe", "wrong")

        assertEquals(AudiobookshelfSignInError.WRONG_CREDENTIALS, viewModel.signInError.value)
        assertFalse(viewModel.isSigningIn.value)
        assertNull(viewModel.account.value)
    }

    @Test
    fun `signing in, then out, keeps the server to sign in again`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAudiobookshelfRepository())

        viewModel.signIn("https://abs.example.com", "abe", "secret")
        assertEquals(AudiobookshelfAccount("https://abs.example.com", "abe", isSignedIn = true), viewModel.account.value)
        assertNull(viewModel.signInError.value)

        viewModel.signOut()
        assertEquals(AudiobookshelfAccount("https://abs.example.com", "abe", isSignedIn = false), viewModel.account.value)
    }
}
