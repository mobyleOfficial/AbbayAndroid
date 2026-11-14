package com.mobyle.abbay.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.Book
import com.model.BookState
import com.statistics.BookStatistics
import com.usecase.ObserveBookStatistics
import com.usecase.ObserveBooks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookStatisticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBooks: ObserveBooks,
    observeBookStatistics: ObserveBookStatistics,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    val book: StateFlow<Book?> = observeBooks().map { books -> books.firstOrNull { it.id == bookId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val statistics: StateFlow<BookStatistics?> = book.filterNotNull()
        .map { if (it.state == BookState.FINISHED) 0L else (it.duration - it.bookPositionMs).coerceAtLeast(0L) }
        .distinctUntilChanged()
        .flatMapLatest { remaining -> observeBookStatistics(bookId, remaining) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
    }
}
