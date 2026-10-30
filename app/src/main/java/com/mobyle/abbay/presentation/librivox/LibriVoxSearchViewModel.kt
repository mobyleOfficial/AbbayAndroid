package com.mobyle.abbay.presentation.librivox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.DataResult
import com.model.LibriVoxBook
import com.usecase.GetNetworkStatus
import com.usecase.GetPopularLibriVoxBooks
import com.usecase.ObserveLibriVoxLibrary
import com.usecase.SearchLibriVox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface LibriVoxSearchState {
    data object Loading : LibriVoxSearchState

    data object Offline : LibriVoxSearchState

    data object Failed : LibriVoxSearchState

    data object NoResults : LibriVoxSearchState

    data class Popular(val books: List<LibriVoxBook>) : LibriVoxSearchState

    data class Results(val books: List<LibriVoxBook>) : LibriVoxSearchState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibriVoxSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPopularBooks: GetPopularLibriVoxBooks,
    private val searchLibriVox: SearchLibriVox,
    private val getNetworkStatus: GetNetworkStatus,
    observeLibriVoxLibrary: ObserveLibriVoxLibrary,
) : ViewModel() {
    private val mutableQuery = MutableStateFlow(savedStateHandle.get<String>(QUERY_ARGUMENT).orEmpty())
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    private val attempt = MutableStateFlow(0)
    private var popularBooks: List<LibriVoxBook> = emptyList()

    val libraryBookIds: StateFlow<Map<String, String>> = observeLibriVoxLibrary()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val state: StateFlow<LibriVoxSearchState> = combine(mutableQuery, attempt) { text, _ -> text.trim() }
        .flatMapLatest { text -> if (text.isEmpty()) popular() else results(text) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LibriVoxSearchState.Loading)

    fun setQuery(text: String) {
        mutableQuery.value = text
    }

    fun retry() {
        attempt.value++
    }

    private fun popular(): Flow<LibriVoxSearchState> = flow {
        if (popularBooks.isNotEmpty()) {
            emit(LibriVoxSearchState.Popular(popularBooks))
            return@flow
        }
        emit(LibriVoxSearchState.Loading)
        val result = getPopularBooks()
        val books = (result as? DataResult.Success)?.value.orEmpty()
        if (books.isEmpty()) {
            emit(unreachable())
        } else {
            popularBooks = books
            emit(LibriVoxSearchState.Popular(books))
        }
    }

    private fun results(text: String): Flow<LibriVoxSearchState> = flow {
        emit(LibriVoxSearchState.Loading)
        if (!getNetworkStatus().isOnline) {
            emit(LibriVoxSearchState.Offline)
            return@flow
        }
        delay(SEARCH_DEBOUNCE_MS)
        when (val result = searchLibriVox(text)) {
            is DataResult.Failure -> emit(unreachable())
            is DataResult.Success -> emit(
                if (result.value.isEmpty()) LibriVoxSearchState.NoResults else LibriVoxSearchState.Results(result.value),
            )
        }
    }

    private fun unreachable() = if (getNetworkStatus().isOnline) LibriVoxSearchState.Failed else LibriVoxSearchState.Offline

    companion object {
        const val QUERY_ARGUMENT = "query"
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
