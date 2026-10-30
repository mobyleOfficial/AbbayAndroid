package com.mobyle.abbay.presentation.bookmarks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.BookDetails
import com.model.Bookmark
import com.usecase.DeleteBookmark
import com.usecase.ObserveBookDetails
import com.usecase.ObserveBookmarks
import com.usecase.RestoreBookmark
import com.usecase.UpdateBookmarkNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bookmarks of one book. Jumping is done by the player's ViewModel. */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBookmarks: ObserveBookmarks,
    observeBookDetails: ObserveBookDetails,
    private val updateBookmarkNote: UpdateBookmarkNote,
    private val deleteBookmark: DeleteBookmark,
    private val restoreBookmark: RestoreBookmark,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    /** Null until the database answers. */
    val bookmarks: StateFlow<List<Bookmark>?> = observeBookmarks(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val details: StateFlow<BookDetails?> = observeBookDetails(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveNote(bookmarkId: Long, note: String) = viewModelScope.launch { updateBookmarkNote(bookmarkId, note) }

    fun delete(bookmark: Bookmark) = viewModelScope.launch { deleteBookmark(bookmark.id) }

    fun undoDelete(bookmark: Bookmark) = viewModelScope.launch { restoreBookmark(bookmark) }

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
    }
}
