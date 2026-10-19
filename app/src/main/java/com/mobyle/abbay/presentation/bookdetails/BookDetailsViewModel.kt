package com.mobyle.abbay.presentation.bookdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.BookDetails
import com.model.BookOverrides
import com.model.CoverCandidate
import com.usecase.GetCoverCandidates
import com.usecase.ObserveBookDetails
import com.usecase.SetSortByTrackNumber
import com.usecase.SetUserCover
import com.usecase.UpdateAppSettings
import com.usecase.UpdateBookOverrides
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Book details and metadata editing. */
@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBookDetails: ObserveBookDetails,
    private val updateBookOverrides: UpdateBookOverrides,
    private val setSortByTrackNumber: SetSortByTrackNumber,
    private val getCoverCandidates: GetCoverCandidates,
    private val setUserCover: SetUserCover,
    private val updateAppSettings: UpdateAppSettings,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    val details: StateFlow<BookDetails?> = observeBookDetails(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _coverCandidates = MutableStateFlow(emptyList<CoverCandidate>())
    val coverCandidates: StateFlow<List<CoverCandidate>> get() = _coverCandidates

    fun saveOverrides(overrides: BookOverrides) = viewModelScope.launch { updateBookOverrides(bookId, overrides) }

    fun changeSortByTrackNumber(enabled: Boolean) = viewModelScope.launch { setSortByTrackNumber(bookId, enabled) }

    fun loadCoverCandidates() = viewModelScope.launch { _coverCandidates.value = getCoverCandidates(bookId) }

    fun chooseCover(imageUri: String) = viewModelScope.launch { setUserCover(bookId, imageUri) }

    /** The image shared back from the browser goes to this book. */
    fun prepareWebCoverSearch() = viewModelScope.launch { updateAppSettings { it.copy(pendingCoverBookId = bookId) } }

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
    }
}
