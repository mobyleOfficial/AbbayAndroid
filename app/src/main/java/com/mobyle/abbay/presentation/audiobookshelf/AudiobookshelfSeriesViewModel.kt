package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.DataResult
import com.model.LibriVoxBook
import com.usecase.GetAudiobookshelfSeriesBooks
import com.usecase.GetNetworkStatus
import com.usecase.ObserveAudiobookshelfLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AudiobookshelfSeriesState {
    data object Loading : AudiobookshelfSeriesState

    data object Offline : AudiobookshelfSeriesState

    data object Failed : AudiobookshelfSeriesState

    data object Empty : AudiobookshelfSeriesState

    data class Books(val books: List<LibriVoxBook>) : AudiobookshelfSeriesState
}

/** The books of one series on the user's Audiobookshelf server, in their order in it. */
@HiltViewModel
class AudiobookshelfSeriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSeriesBooks: GetAudiobookshelfSeriesBooks,
    private val getNetworkStatus: GetNetworkStatus,
    observeAudiobookshelfLibrary: ObserveAudiobookshelfLibrary,
) : ViewModel() {
    private val libraryId = savedStateHandle.get<String>(LIBRARY_ARGUMENT).orEmpty()
    private val seriesId = savedStateHandle.get<String>(SERIES_ARGUMENT).orEmpty()
    val name: String = savedStateHandle.get<String>(NAME_ARGUMENT).orEmpty()

    private val mutableState = MutableStateFlow<AudiobookshelfSeriesState>(AudiobookshelfSeriesState.Loading)
    val state: StateFlow<AudiobookshelfSeriesState> = mutableState.asStateFlow()

    val libraryBookIds: StateFlow<Map<String, String>> = observeAudiobookshelfLibrary()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        load()
    }

    fun load() {
        mutableState.value = AudiobookshelfSeriesState.Loading
        viewModelScope.launch {
            mutableState.value = when (val result = getSeriesBooks(libraryId, seriesId)) {
                is DataResult.Failure ->
                    if (getNetworkStatus().isOnline) AudiobookshelfSeriesState.Failed else AudiobookshelfSeriesState.Offline
                is DataResult.Success ->
                    if (result.value.isEmpty()) AudiobookshelfSeriesState.Empty else AudiobookshelfSeriesState.Books(result.value)
            }
        }
    }

    companion object {
        const val LIBRARY_ARGUMENT = "library"
        const val SERIES_ARGUMENT = "series"
        const val NAME_ARGUMENT = "name"
    }
}
