package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfSignIn
import com.model.AudiobookshelfSignInError
import com.model.DataResult
import com.model.LibriVoxBook
import com.usecase.GetAudiobookshelfLibraries
import com.usecase.GetNetworkStatus
import com.usecase.GetRecentAudiobookshelfBooks
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.ObserveAudiobookshelfLibrary
import com.usecase.SearchAudiobookshelf
import com.usecase.SignInAudiobookshelf
import com.usecase.SignOutAudiobookshelf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AudiobookshelfBrowseState {
    data object Loading : AudiobookshelfBrowseState

    data object Offline : AudiobookshelfBrowseState

    data object Failed : AudiobookshelfBrowseState

    data object NoLibraries : AudiobookshelfBrowseState

    data object NoResults : AudiobookshelfBrowseState

    data class Recent(val books: List<LibriVoxBook>) : AudiobookshelfBrowseState

    data class Results(val books: List<LibriVoxBook>) : AudiobookshelfBrowseState
}

/** Signs in to the user's Audiobookshelf server, then browses its newest books or searches a library of it. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AudiobookshelfViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeAccount: ObserveAudiobookshelfAccount,
    private val signInAudiobookshelf: SignInAudiobookshelf,
    private val signOutAudiobookshelf: SignOutAudiobookshelf,
    private val getLibraries: GetAudiobookshelfLibraries,
    private val getRecentBooks: GetRecentAudiobookshelfBooks,
    private val searchAudiobookshelf: SearchAudiobookshelf,
    private val getNetworkStatus: GetNetworkStatus,
    observeAudiobookshelfLibrary: ObserveAudiobookshelfLibrary,
) : ViewModel() {
    val account: StateFlow<AudiobookshelfAccount?> = observeAccount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = mutableSigningIn.asStateFlow()

    private val mutableSignInError = MutableStateFlow<AudiobookshelfSignInError?>(null)
    val signInError: StateFlow<AudiobookshelfSignInError?> = mutableSignInError.asStateFlow()

    private val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    private val mutableSelectedLibraryId = MutableStateFlow(savedStateHandle.get<String>(LIBRARY_KEY))
    private val attempt = MutableStateFlow(0)
    private val recentByLibrary = mutableMapOf<String, List<LibriVoxBook>>()

    val libraryBookIds: StateFlow<Map<String, String>> = observeAudiobookshelfLibrary()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val signedInServer: Flow<String?> = account.map { it?.takeIf(AudiobookshelfAccount::isSignedIn)?.serverUrl }
        .distinctUntilChanged()

    private val librariesResult: StateFlow<DataResult<List<AudiobookshelfLibrary>>?> =
        combine(signedInServer, attempt) { server, _ -> server }
            .flatMapLatest { server -> if (server == null) flowOf(null) else librariesLoading() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val libraries: StateFlow<List<AudiobookshelfLibrary>> = librariesResult
        .map { (it as? DataResult.Success)?.value.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedLibraryId: StateFlow<String?> = combine(libraries, mutableSelectedLibraryId) { all, selected ->
        selected?.takeIf { id -> all.any { it.id == id } } ?: all.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<AudiobookshelfBrowseState> = combine(librariesResult, selectedLibraryId, mutableQuery) { result, libraryId, text ->
        Triple(result, libraryId, text.trim())
    }
        .distinctUntilChanged()
        .flatMapLatest { (result, libraryId, text) ->
            when {
                result == null -> flowOf(AudiobookshelfBrowseState.Loading)
                result is DataResult.Failure -> flowOf(unreachable())
                libraryId == null -> flowOf(AudiobookshelfBrowseState.NoLibraries)
                text.isEmpty() -> recent(libraryId)
                else -> results(libraryId, text)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudiobookshelfBrowseState.Loading)

    fun signIn(serverUrl: String, username: String, password: String) {
        if (mutableSigningIn.value) return
        mutableSigningIn.value = true
        mutableSignInError.value = null
        viewModelScope.launch {
            when (val result = signInAudiobookshelf(serverUrl, username, password)) {
                is AudiobookshelfSignIn.Success -> recentByLibrary.clear()
                is AudiobookshelfSignIn.Failure -> mutableSignInError.value = result.error
            }
            mutableSigningIn.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutAudiobookshelf()
            recentByLibrary.clear()
            mutableQuery.value = ""
        }
    }

    fun setQuery(text: String) {
        mutableQuery.value = text
    }

    fun selectLibrary(libraryId: String) {
        savedStateHandle[LIBRARY_KEY] = libraryId
        mutableSelectedLibraryId.value = libraryId
    }

    fun retry() {
        attempt.value++
    }

    private fun librariesLoading(): Flow<DataResult<List<AudiobookshelfLibrary>>?> = flow {
        emit(null)
        emit(getLibraries())
    }

    private fun recent(libraryId: String): Flow<AudiobookshelfBrowseState> = flow {
        recentByLibrary[libraryId]?.let {
            emit(AudiobookshelfBrowseState.Recent(it))
            return@flow
        }
        emit(AudiobookshelfBrowseState.Loading)
        when (val result = getRecentBooks(libraryId)) {
            is DataResult.Failure -> emit(unreachable())
            is DataResult.Success -> {
                recentByLibrary[libraryId] = result.value
                emit(if (result.value.isEmpty()) AudiobookshelfBrowseState.NoResults else AudiobookshelfBrowseState.Recent(result.value))
            }
        }
    }

    private fun results(libraryId: String, text: String): Flow<AudiobookshelfBrowseState> = flow {
        emit(AudiobookshelfBrowseState.Loading)
        delay(SEARCH_DEBOUNCE_MS)
        when (val result = searchAudiobookshelf(libraryId, text)) {
            is DataResult.Failure -> emit(unreachable())
            is DataResult.Success -> emit(
                if (result.value.isEmpty()) AudiobookshelfBrowseState.NoResults else AudiobookshelfBrowseState.Results(result.value),
            )
        }
    }

    private fun unreachable() = if (getNetworkStatus().isOnline) AudiobookshelfBrowseState.Failed else AudiobookshelfBrowseState.Offline

    private companion object {
        const val LIBRARY_KEY = "library"
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
