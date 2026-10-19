package com.mobyle.abbay.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.Book
import com.statistics.GlobalStatistics
import com.usecase.ObserveAppSettings
import com.usecase.ObserveBooks
import com.usecase.ObserveGlobalStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The global statistics screen. */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    observeGlobalStatistics: ObserveGlobalStatistics,
    observeBooks: ObserveBooks,
    observeAppSettings: ObserveAppSettings,
) : ViewModel() {
    /** Null until the database answers. */
    val statistics: StateFlow<GlobalStatistics?> = observeGlobalStatistics().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Books by id, to name the top books. */
    val books: StateFlow<Map<String, Book>> = observeBooks().map { books -> books.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val isRecording: StateFlow<Boolean> = observeAppSettings().map { it.statisticsEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
}
