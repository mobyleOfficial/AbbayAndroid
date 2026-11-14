package com.mobyle.abbay.presentation.librivox

import androidx.lifecycle.SavedStateHandle
import com.model.LibriVoxBook
import com.usecase.AddLibriVoxBook
import com.usecase.GetNetworkStatus
import com.usecase.ObserveLibriVoxDetails
import com.usecase.ObserveLibriVoxDownloads
import com.usecase.ObserveLibriVoxLibrary
import com.usecase.PauseLibriVoxDownload
import com.usecase.RemoveLibriVoxDownload
import com.usecase.StartLibriVoxDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LibriVoxBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetails: ObserveLibriVoxDetails,
    addLibriVoxBook: AddLibriVoxBook,
    startLibriVoxDownload: StartLibriVoxDownload,
    pauseLibriVoxDownload: PauseLibriVoxDownload,
    removeLibriVoxDownload: RemoveLibriVoxDownload,
    getNetworkStatus: GetNetworkStatus,
    observeLibriVoxLibrary: ObserveLibriVoxLibrary,
    observeLibriVoxDownloads: ObserveLibriVoxDownloads,
) : CatalogBookViewModel(
    source = CatalogSource(
        book = LibriVoxBook(
            id = savedStateHandle.get<String>(ID_ARGUMENT).orEmpty(),
            title = savedStateHandle.get<String>(TITLE_ARGUMENT).orEmpty(),
            authors = savedStateHandle.get<String>(AUTHORS_ARGUMENT).orEmpty(),
            creator = savedStateHandle.get<String>(CREATOR_ARGUMENT)?.takeIf(String::isNotEmpty),
            coverUrl = LibriVoxBook.coverUrlOf(savedStateHandle.get<String>(ID_ARGUMENT).orEmpty()),
        ),
        details = observeDetails::invoke,
        add = addLibriVoxBook::invoke,
        libraryBookIds = observeLibriVoxLibrary(),
    ),
    downloads = CatalogDownloads(
        downloads = observeLibriVoxDownloads(),
        start = startLibriVoxDownload,
        pause = pauseLibriVoxDownload,
        remove = removeLibriVoxDownload,
        getNetworkStatus = getNetworkStatus,
    ),
) {
    companion object {
        const val ID_ARGUMENT = "id"
        const val TITLE_ARGUMENT = "title"
        const val AUTHORS_ARGUMENT = "authors"
        const val CREATOR_ARGUMENT = "creator"
    }
}
