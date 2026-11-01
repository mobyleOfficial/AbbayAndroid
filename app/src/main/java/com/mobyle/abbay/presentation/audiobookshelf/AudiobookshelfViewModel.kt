package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfSeries
import com.model.AudiobookshelfSort
import com.model.DataResult
import com.model.LibriVoxBook
import com.usecase.GetAudiobookshelfBooks
import com.usecase.GetAudiobookshelfLibraries
import com.usecase.GetAudiobookshelfSeries
import com.usecase.GetNetworkStatus
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.ObserveAudiobookshelfLibrary
import com.usecase.SearchAudiobookshelf
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AudiobookshelfBrowseState {
    data object Loading : AudiobookshelfBrowseState

    data object Offline : AudiobookshelfBrowseState

    data object Failed : AudiobookshelfBrowseState

    data object NoLibraries : AudiobookshelfBrowseState

    data object NoResults : AudiobookshelfBrowseState

    /** A library's books, page by page. */
    data class Books(val books: List<LibriVoxBook>, val more: MoreItems) : AudiobookshelfBrowseState

    data class Results(val books: List<LibriVoxBook>) : AudiobookshelfBrowseState

    /** A library's series, page by page. */
    data class Series(val series: List<AudiobookshelfSeries>, val more: MoreItems) : AudiobookshelfBrowseState
}

/** What a library shows before a search: all its books, or its series. */
enum class AudiobookshelfTab {
    ALL,
    SERIES,
}

/** Browses a library of the signed-in Audiobookshelf server (all its books, its series) or searches it. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AudiobookshelfViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeAccount: ObserveAudiobookshelfAccount,
    private val getLibraries: GetAudiobookshelfLibraries,
    private val getBooks: GetAudiobookshelfBooks,
    private val getSeries: GetAudiobookshelfSeries,
    private val searchAudiobookshelf: SearchAudiobookshelf,
    private val getNetworkStatus: GetNetworkStatus,
    observeAudiobookshelfLibrary: ObserveAudiobookshelfLibrary,
) : ViewModel() {
    private val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    private val mutableTab = MutableStateFlow(savedStateHandle.get<AudiobookshelfTab>(TAB_KEY) ?: AudiobookshelfTab.ALL)
    val tab: StateFlow<AudiobookshelfTab> = mutableTab.asStateFlow()

    private val mutableSort = MutableStateFlow(savedStateHandle.get<AudiobookshelfSort>(SORT_KEY) ?: AudiobookshelfSort.RECENTLY_ADDED)
    val sort: StateFlow<AudiobookshelfSort> = mutableSort.asStateFlow()

    private val mutableSelectedLibraryId = MutableStateFlow(savedStateHandle.get<String>(LIBRARY_KEY))
    private val attempt = MutableStateFlow(0)

    // Each list keeps its pages while the screen is open; another server starts them over.
    private val bookShelves = mutableMapOf<Pair<String, AudiobookshelfSort>, PagedShelf<LibriVoxBook>>()
    private val seriesShelves = mutableMapOf<String, PagedShelf<AudiobookshelfSeries>>()
    private var shown: PagedShelf<*>? = null

    val libraryBookIds: StateFlow<Map<String, String>> = observeAudiobookshelfLibrary()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val signedInServer: Flow<String?> = observeAccount()
        .map { it?.takeIf(AudiobookshelfAccount::isSignedIn)?.serverUrl }
        .distinctUntilChanged()
        .onEach {
            bookShelves.clear()
            seriesShelves.clear()
        }

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

    private data class Browsing(
        val libraries: DataResult<List<AudiobookshelfLibrary>>?,
        val libraryId: String?,
        val text: String,
        val tab: AudiobookshelfTab,
        val sort: AudiobookshelfSort,
    )

    val state: StateFlow<AudiobookshelfBrowseState> =
        combine(librariesResult, selectedLibraryId, mutableQuery, mutableTab, mutableSort) { result, libraryId, text, tab, sort ->
            Browsing(result, libraryId, text.trim(), tab, sort)
        }
            .distinctUntilChanged()
            .flatMapLatest { browsing ->
                val libraryId = browsing.libraryId
                shown = null
                when {
                    browsing.libraries == null -> flowOf(AudiobookshelfBrowseState.Loading)
                    browsing.libraries is DataResult.Failure -> flowOf(unreachable())
                    libraryId == null -> flowOf(AudiobookshelfBrowseState.NoLibraries)
                    browsing.text.isNotEmpty() -> results(libraryId, browsing.text)
                    browsing.tab == AudiobookshelfTab.SERIES -> {
                        val shelf = seriesShelves.getOrPut(libraryId) { PagedShelf(viewModelScope) { page -> getSeries(libraryId, page) } }
                        shown = shelf
                        shelf.state.map { it.toBrowseState(AudiobookshelfBrowseState::Series) }
                    }
                    else -> {
                        val shelf = bookShelves.getOrPut(libraryId to browsing.sort) {
                            PagedShelf(viewModelScope) { page -> getBooks(libraryId, browsing.sort, page) }
                        }
                        shown = shelf
                        shelf.state.map { it.toBrowseState(AudiobookshelfBrowseState::Books) }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AudiobookshelfBrowseState.Loading)

    fun setQuery(text: String) {
        mutableQuery.value = text
    }

    fun selectLibrary(libraryId: String) {
        savedStateHandle[LIBRARY_KEY] = libraryId
        mutableSelectedLibraryId.value = libraryId
    }

    fun selectTab(tab: AudiobookshelfTab) {
        savedStateHandle[TAB_KEY] = tab
        mutableTab.value = tab
    }

    fun selectSort(sort: AudiobookshelfSort) {
        savedStateHandle[SORT_KEY] = sort
        mutableSort.value = sort
    }

    /** The next page of the list shown, as it scrolls near its end. */
    fun loadMore() {
        shown?.loadMore()
    }

    /** Tries again what failed: the list's last page, or the server itself. */
    fun retry() {
        val shelf = shown
        if (shelf != null) shelf.loadMore() else attempt.value++
    }

    private fun librariesLoading(): Flow<DataResult<List<AudiobookshelfLibrary>>?> = flow {
        emit(null)
        emit(getLibraries())
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

    /** A list's first page shows full screen while it loads or fails; later pages show at its end. */
    private fun <T> PagedShelf.State<T>.toBrowseState(shown: (List<T>, MoreItems) -> AudiobookshelfBrowseState) = when {
        items.isNotEmpty() -> shown(items, more)
        more == MoreItems.LOADING -> AudiobookshelfBrowseState.Loading
        more == MoreItems.FAILED -> unreachable()
        else -> AudiobookshelfBrowseState.NoResults
    }

    private companion object {
        const val LIBRARY_KEY = "library"
        const val TAB_KEY = "tab"
        const val SORT_KEY = "sort"
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
