package com.mobyle.abbay.presentation.librivox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librivox.CellularDownloadPolicy
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDownload
import com.usecase.AddLibriVoxBook
import com.usecase.GetNetworkStatus
import com.usecase.ObserveLibriVoxDetails
import com.usecase.ObserveLibriVoxDownloads
import com.usecase.ObserveLibriVoxLibrary
import com.usecase.PauseLibriVoxDownload
import com.usecase.RemoveLibriVoxDownload
import com.usecase.StartLibriVoxDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibriVoxAddMode {
    DOWNLOAD,
    STREAM,
}

sealed interface LibriVoxBookState {
    val book: LibriVoxBook

    data class Placeholder(override val book: LibriVoxBook) : LibriVoxBookState

    data class Loaded(override val book: LibriVoxBook, val isAwaitingTitles: Boolean) : LibriVoxBookState

    data class Failed(override val book: LibriVoxBook) : LibriVoxBookState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibriVoxBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDetails: ObserveLibriVoxDetails,
    private val addLibriVoxBook: AddLibriVoxBook,
    private val startLibriVoxDownload: StartLibriVoxDownload,
    private val pauseLibriVoxDownload: PauseLibriVoxDownload,
    private val removeLibriVoxDownload: RemoveLibriVoxDownload,
    private val getNetworkStatus: GetNetworkStatus,
    observeLibriVoxLibrary: ObserveLibriVoxLibrary,
    observeLibriVoxDownloads: ObserveLibriVoxDownloads,
) : ViewModel() {
    private val lightBook = LibriVoxBook(
        id = savedStateHandle.get<String>(ID_ARGUMENT).orEmpty(),
        title = savedStateHandle.get<String>(TITLE_ARGUMENT).orEmpty(),
        authors = savedStateHandle.get<String>(AUTHORS_ARGUMENT).orEmpty(),
        creator = savedStateHandle.get<String>(CREATOR_ARGUMENT)?.takeIf(String::isNotEmpty),
        coverUrl = LibriVoxBook.coverUrlOf(savedStateHandle.get<String>(ID_ARGUMENT).orEmpty()),
    )

    private val mutableState = MutableStateFlow<LibriVoxBookState>(LibriVoxBookState.Placeholder(lightBook))
    val state: StateFlow<LibriVoxBookState> = mutableState.asStateFlow()

    val libraryBookId: StateFlow<String?> = observeLibriVoxLibrary().map { it[lightBook.id] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val download: StateFlow<LibriVoxDownload?> = combine(libraryBookId, observeLibriVoxDownloads()) { bookId, downloads ->
        bookId?.let { downloads[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableStarting = MutableStateFlow<LibriVoxAddMode?>(null)
    val starting: StateFlow<LibriVoxAddMode?> = mutableStarting.asStateFlow()

    private val mutableCellularPromptBytes = MutableStateFlow<Long?>(null)
    val cellularPromptBytes: StateFlow<Long?> = mutableCellularPromptBytes.asStateFlow()

    private val addFailures = Channel<Unit>(Channel.CONFLATED)
    val addFailed: Flow<Unit> = addFailures.receiveAsFlow()

    private val listenRequests = Channel<String>(Channel.CONFLATED)
    val listen: Flow<String> = listenRequests.receiveAsFlow()

    private var pendingAction: LibriVoxAddMode? = null
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        mutableState.value = LibriVoxBookState.Placeholder(state.value.book)
        loadJob = viewModelScope.launch {
            observeDetails(lightBook).collect { result ->
                mutableState.value = when (result) {
                    is DataResult.Success -> LibriVoxBookState.Loaded(result.value.book, result.value.isAwaitingTitles)
                    is DataResult.Failure -> (state.value as? LibriVoxBookState.Loaded)?.copy(isAwaitingTitles = false)
                        ?: LibriVoxBookState.Failed(state.value.book)
                }
            }
            val loaded = state.value as? LibriVoxBookState.Loaded
            if (loaded?.isAwaitingTitles == true) mutableState.value = loaded.copy(isAwaitingTitles = false)
            val pending = pendingAction ?: return@launch
            pendingAction = null
            mutableStarting.value = null
            when (pending) {
                LibriVoxAddMode.DOWNLOAD -> download()
                LibriVoxAddMode.STREAM -> listenWithoutDownloading()
            }
        }
    }

    fun download() {
        val loaded = readyToAdd(LibriVoxAddMode.DOWNLOAD) ?: return
        when (val decision = CellularDownloadPolicy.decide(loaded.book.totalBytes, getNetworkStatus())) {
            is CellularDownloadPolicy.Decision.Proceed -> start(loaded.book, decision.allowsCellular)
            CellularDownloadPolicy.Decision.AskUser -> mutableCellularPromptBytes.value = loaded.book.totalBytes
        }
    }

    fun answerCellularPrompt(useMobileData: Boolean?) {
        mutableCellularPromptBytes.value = null
        val loaded = state.value as? LibriVoxBookState.Loaded ?: return
        if (useMobileData != null) start(loaded.book, allowsCellular = useMobileData)
    }

    fun listenWithoutDownloading() {
        val loaded = readyToAdd(LibriVoxAddMode.STREAM) ?: return
        mutableStarting.value = LibriVoxAddMode.STREAM
        viewModelScope.launch {
            when (val result = addLibriVoxBook(loaded.book, allowsCellular = false, download = false)) {
                is DataResult.Success -> listenRequests.trySend(result.value)
                is DataResult.Failure -> addFailures.trySend(Unit)
            }
            mutableStarting.value = null
        }
    }

    private fun readyToAdd(mode: LibriVoxAddMode): LibriVoxBookState.Loaded? {
        val loaded = state.value as? LibriVoxBookState.Loaded ?: return null
        if (libraryBookId.value != null || mutableStarting.value != null) return null
        if (loaded.isAwaitingTitles) {
            pendingAction = mode
            mutableStarting.value = mode
            return null
        }
        return loaded
    }

    fun resumeDownload() {
        val bookId = libraryBookId.value ?: return
        val decision = CellularDownloadPolicy.decide(state.value.book.totalBytes, getNetworkStatus())
        val allowsCellular = (decision as? CellularDownloadPolicy.Decision.Proceed)?.allowsCellular ?: true
        viewModelScope.launch { startLibriVoxDownload(bookId, allowsCellular) }
    }

    fun pauseDownload() {
        val bookId = libraryBookId.value ?: return
        viewModelScope.launch { pauseLibriVoxDownload(bookId) }
    }

    fun removeDownload() {
        val bookId = libraryBookId.value ?: return
        viewModelScope.launch { removeLibriVoxDownload(bookId) }
    }

    private fun start(book: LibriVoxBook, allowsCellular: Boolean) {
        mutableStarting.value = LibriVoxAddMode.DOWNLOAD
        viewModelScope.launch {
            if (addLibriVoxBook(book, allowsCellular) is DataResult.Failure) addFailures.trySend(Unit)
            mutableStarting.value = null
        }
    }

    companion object {
        const val ID_ARGUMENT = "id"
        const val TITLE_ARGUMENT = "title"
        const val AUTHORS_ARGUMENT = "authors"
        const val CREATOR_ARGUMENT = "creator"
    }
}
