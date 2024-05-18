package com.mobyle.abbay.presentation.booklist

import android.util.Log
import com.mobyle.abbay.infra.common.BaseViewModel
import com.model.Book
import com.model.BookFile
import com.model.BookFolder
import com.usecase.UpsertBookList
import com.usecase.GetBooksList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val getBooksList: GetBooksList,
    private val upsertBookList: UpsertBookList
) :
    BaseViewModel() {
    private val _uiState = MutableStateFlow<BooksListUiState>(BooksListUiState.Loading)
    val uiState: StateFlow<BooksListUiState> get() = _uiState
    var booksList = mutableListOf<Book>()
        private set

    val isPlaying = MutableStateFlow(false)

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> get() = _selectedBook

    private val _currentProgress = MutableStateFlow(0L)
    val currentProgress: StateFlow<Long> get() = _currentProgress

    var hasBookSelected = _selectedBook.map { it != null }

    init {
        getAudiobookList()
    }

    fun updateBookList(booksList: List<BookFile>) = launch {
        this.booksList.addAll(booksList)
        upsertBookList.invoke(booksList)
        val newBookList = mutableListOf<Book>()
        newBookList.addAll(this.booksList)
        _uiState.emit(BooksListUiState.BookListSuccess(newBookList))
    }

    fun addBookFolder(bookFolder: BookFolder) = launch {
        this.booksList.add(bookFolder)
        upsertBookList.invoke(booksList)
        val newBookList = mutableListOf<Book>()
        newBookList.addAll(this.booksList)
        _uiState.emit(BooksListUiState.BookListSuccess(newBookList))
    }

    fun updateBookProgress(id: String, progress: Long) {
        booksList.firstOrNull { it.id == id }?.let {

            when (it) {
                is BookFile -> {
                    booksList[booksList.indexOf(it)] = it.copy(progress = progress)
                }

                is BookFolder -> {}
                else -> {}
            }
        }

        _uiState.value = BooksListUiState.BookListSuccess(booksList.toList())
    }

    fun updateBookList(id: String, progress: Long) = launch {
        updateBookProgress(id, progress)
        upsertBookList.invoke(booksList)
    }

    fun selectBook(book: Book?) {
        _selectedBook.tryEmit(book)
    }

    fun setCurrentProgress(progress: Long) {
        _currentProgress.tryEmit(progress)
    }

    private fun getAudiobookList() = launch {
        val booksList = getBooksList.invoke()
        this.booksList.addAll(booksList)

        val state = if (booksList.isEmpty()) {
            BooksListUiState.NoBookSelected
        } else {
            BooksListUiState.BookListSuccess(booksList)
        }

        _uiState.emit(state)
    }

    sealed class BooksListUiState {
        data class BookListSuccess(val audiobookList: List<Book>) :
            BooksListUiState()

        data object NoBookSelected : BooksListUiState()
        data object GenericError : BooksListUiState()
        data object Loading : BooksListUiState()
    }
}