package com.mobyle.abbay.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.Book
import com.usecase.ObserveBooks
import com.usecase.SearchBooks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(observeBooks: ObserveBooks, searchBooks: SearchBooks) : ViewModel() {
    private val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    val results: StateFlow<List<Book>> = combine(observeBooks(), mutableQuery) { books, text -> searchBooks(books, text) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(text: String) {
        mutableQuery.value = text
    }
}
