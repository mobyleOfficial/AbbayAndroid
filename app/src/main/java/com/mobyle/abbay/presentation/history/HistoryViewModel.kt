package com.mobyle.abbay.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.HistoryEvent
import com.usecase.ObserveHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(savedStateHandle: SavedStateHandle, observeHistory: ObserveHistory) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    val events: StateFlow<List<HistoryEvent>?> = observeHistory(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
    }
}
