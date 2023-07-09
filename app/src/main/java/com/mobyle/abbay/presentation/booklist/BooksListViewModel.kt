package com.mobyle.abbay.presentation.booklist

import com.mobyle.abbay.infra.common.BaseViewModel
import com.model.Book
import com.model.BookFile
import com.model.BookFolder
import com.usecase.AddBooks
import com.usecase.GetBooksList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val getBooksList: GetBooksList,
    private val addBooks: AddBooks
) :
    BaseViewModel() {
    private val _uiState = MutableStateFlow<BooksListUiState>(BooksListUiState.Loading)
    val uiState: StateFlow<BooksListUiState> get() = _uiState
    private val booksList = mutableListOf<Book>()

    init {
        getAudiobookList()
    }

    fun addBooksList(booksList: List<BookFile>) = launch {
        this.booksList.addAll(booksList)
        addBooks.invoke(booksList)
        _uiState.tryEmit(BooksListUiState.BookListSuccess(this.booksList))
    }

    fun addBookFolder(bookFolder: BookFolder) = launch {
        this.booksList.add(bookFolder)
        addBooks.invoke(booksList)
        _uiState.tryEmit(BooksListUiState.BookListSuccess(this.booksList))
    }

    private fun getAudiobookList() = launch {
        val booksList = getBooksList.invoke()

        val state = if (booksList.isEmpty()) {
            BooksListUiState.NoBookSelected
        } else {
            this.booksList.addAll(booksList)
            BooksListUiState.BookListSuccess(booksList)
        }

        _uiState.tryEmit(state)
    }

    sealed class BooksListUiState {
        data class BookListSuccess(val audiobookList: List<Book>) :
            BooksListUiState()

        object NoBookSelected : BooksListUiState()
        object GenericError : BooksListUiState()
        object Loading : BooksListUiState()
    }
}