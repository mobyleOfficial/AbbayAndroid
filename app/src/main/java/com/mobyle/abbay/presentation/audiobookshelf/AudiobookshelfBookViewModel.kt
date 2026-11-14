package com.mobyle.abbay.presentation.audiobookshelf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mobyle.abbay.presentation.librivox.CatalogBookViewModel
import com.mobyle.abbay.presentation.librivox.CatalogDownloads
import com.mobyle.abbay.presentation.librivox.CatalogSource
import com.model.AudiobookshelfAccount
import com.model.LibriVoxBook
import com.usecase.AddAudiobookshelfBook
import com.usecase.GetAudiobookshelfCoverUrl
import com.usecase.GetNetworkStatus
import com.usecase.ObserveAudiobookshelfAccount
import com.usecase.ObserveAudiobookshelfDetails
import com.usecase.ObserveAudiobookshelfLibrary
import com.usecase.ObserveLibriVoxDownloads
import com.usecase.PauseLibriVoxDownload
import com.usecase.RemoveLibriVoxDownload
import com.usecase.StartLibriVoxDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** A book on the user's Audiobookshelf server; once in the library its download works like a LibriVox one. */
@HiltViewModel
class AudiobookshelfBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCoverUrl: GetAudiobookshelfCoverUrl,
    observeAccount: ObserveAudiobookshelfAccount,
    observeDetails: ObserveAudiobookshelfDetails,
    addAudiobookshelfBook: AddAudiobookshelfBook,
    startLibriVoxDownload: StartLibriVoxDownload,
    pauseLibriVoxDownload: PauseLibriVoxDownload,
    removeLibriVoxDownload: RemoveLibriVoxDownload,
    getNetworkStatus: GetNetworkStatus,
    observeAudiobookshelfLibrary: ObserveAudiobookshelfLibrary,
    observeLibriVoxDownloads: ObserveLibriVoxDownloads,
) : CatalogBookViewModel(
    source = CatalogSource(
        book = LibriVoxBook(
            id = savedStateHandle.get<String>(ID_ARGUMENT).orEmpty(),
            title = savedStateHandle.get<String>(TITLE_ARGUMENT).orEmpty(),
            authors = savedStateHandle.get<String>(AUTHORS_ARGUMENT).orEmpty(),
            creator = null,
            coverUrl = getCoverUrl(savedStateHandle.get<String>(ID_ARGUMENT).orEmpty()),
        ),
        details = observeDetails::invoke,
        add = addAudiobookshelfBook::invoke,
        libraryBookIds = observeAudiobookshelfLibrary(),
    ),
    downloads = CatalogDownloads(
        downloads = observeLibriVoxDownloads(),
        start = startLibriVoxDownload,
        pause = pauseLibriVoxDownload,
        remove = removeLibriVoxDownload,
        getNetworkStatus = getNetworkStatus,
    ),
) {
    val account: StateFlow<AudiobookshelfAccount?> = observeAccount().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        const val ID_ARGUMENT = "id"
        const val TITLE_ARGUMENT = "title"
        const val AUTHORS_ARGUMENT = "authors"
    }
}
