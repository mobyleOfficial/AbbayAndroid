package com.mobyle.abbay.presentation.bookdetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.BookDetails
import com.model.BookNameSource
import com.model.BookOverrides
import com.model.CoverCandidate
import com.usecase.GetCoverCandidates
import com.usecase.ObserveBookDetails
import com.usecase.SetBookNameSource
import com.usecase.SetSortByTrackNumber
import com.usecase.SetUserCover
import com.usecase.UpdateAppSettings
import com.usecase.UpdateBookOverrides
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBookDetails: ObserveBookDetails,
    private val updateBookOverrides: UpdateBookOverrides,
    private val setSortByTrackNumber: SetSortByTrackNumber,
    private val setBookNameSource: SetBookNameSource,
    private val getCoverCandidates: GetCoverCandidates,
    private val setUserCover: SetUserCover,
    private val updateAppSettings: UpdateAppSettings,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    val details: StateFlow<BookDetails?> = observeBookDetails(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _coverCandidates = MutableStateFlow(emptyList<CoverCandidate>())
    val coverCandidates: StateFlow<List<CoverCandidate>> get() = _coverCandidates

    var draft by mutableStateOf<BookOverrides?>(null)
        private set

    init {
        viewModelScope.launch {
            var shownTitle: String? = null
            details.filterNotNull().collect { current ->
                val edited = draft
                draft = when {
                    edited == null -> current.toOverrides()
                    // The title changed under an untouched field (e.g. the name source was switched): show the new one.
                    edited.title == shownTitle -> edited.copy(title = current.title)
                    else -> edited
                }
                shownTitle = current.title
            }
        }
    }

    fun editDraft(edited: BookOverrides) {
        draft = edited
    }

    fun saveChanges() {
        val edited = draft?.normalized() ?: return
        val current = details.value ?: return
        val shown = current.toOverrides().normalized()
        val stored = current.overrides.normalized()
        // A field left as shown keeps its stored override, so an untouched title keeps following the name source
        // instead of being frozen as the user's own title.
        val overrides = BookOverrides(
            title = keptOrEdited(edited.title, shown.title, stored.title),
            author = keptOrEdited(edited.author, shown.author, stored.author),
            narrator = keptOrEdited(edited.narrator, shown.narrator, stored.narrator),
            description = keptOrEdited(edited.description, shown.description, stored.description),
        )
        if (overrides == stored) return
        viewModelScope.launch(NonCancellable) { updateBookOverrides(bookId, overrides) }
    }

    fun changeSortByTrackNumber(enabled: Boolean) = viewModelScope.launch { setSortByTrackNumber(bookId, enabled) }

    fun changeNameSource(source: BookNameSource) = viewModelScope.launch { setBookNameSource(bookId, source) }

    fun loadCoverCandidates() = viewModelScope.launch { _coverCandidates.value = getCoverCandidates(bookId) }

    fun chooseCover(imageUri: String) = viewModelScope.launch { setUserCover(bookId, imageUri) }

    fun prepareWebCoverSearch() = viewModelScope.launch { updateAppSettings { it.copy(pendingCoverBookId = bookId) } }

    private fun keptOrEdited(edited: String?, shown: String?, stored: String?): String? = if (edited == shown) stored else edited

    private fun BookDetails.toOverrides() = BookOverrides(title, author.orEmpty(), narrator.orEmpty(), description.orEmpty())

    private fun BookOverrides.normalized() = BookOverrides(
        title = title?.trim()?.takeIf(String::isNotEmpty),
        author = author?.trim()?.takeIf(String::isNotEmpty),
        narrator = narrator?.trim()?.takeIf(String::isNotEmpty),
        description = description?.trim()?.takeIf(String::isNotEmpty),
    )

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
    }
}
