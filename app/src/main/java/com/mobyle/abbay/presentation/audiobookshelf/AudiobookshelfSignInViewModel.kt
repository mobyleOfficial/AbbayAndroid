package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfSignIn
import com.model.AudiobookshelfSignInError
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.SignInAudiobookshelf
import com.usecase.SignOutAudiobookshelf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The account on the user's Audiobookshelf server: signing in, and out. */
@HiltViewModel
class AudiobookshelfSignInViewModel @Inject constructor(
    observeAccount: ObserveAudiobookshelfAccount,
    private val signInAudiobookshelf: SignInAudiobookshelf,
    private val signOutAudiobookshelf: SignOutAudiobookshelf,
) : ViewModel() {
    val account: StateFlow<AudiobookshelfAccount?> = observeAccount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = mutableSigningIn.asStateFlow()

    private val mutableSignInError = MutableStateFlow<AudiobookshelfSignInError?>(null)
    val signInError: StateFlow<AudiobookshelfSignInError?> = mutableSignInError.asStateFlow()

    fun signIn(serverUrl: String, username: String, password: String) {
        if (mutableSigningIn.value) return
        mutableSigningIn.value = true
        mutableSignInError.value = null
        viewModelScope.launch {
            val result = signInAudiobookshelf(serverUrl, username, password)
            if (result is AudiobookshelfSignIn.Failure) mutableSignInError.value = result.error
            mutableSigningIn.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch { signOutAudiobookshelf() }
    }
}
