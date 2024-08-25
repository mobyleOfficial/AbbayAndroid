package com.mobyle.abbay.presentation.booklist

import com.mobyle.abbay.infra.common.BaseViewModel
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.usecase.GetBooksList
import com.usecase.UpsertBookList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    fun addAllBookTypes(filesList: List<Book>) = launch {
        this.booksList.addAll(filesList)
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

                is MultipleBooks -> {}
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

    fun setCurrentProgress(id: String, progress: Long) {
        _currentProgress.tryEmit(progress)
        val index = booksList.indexOfFirst { it.id == id }

        if (index != -1) {
            val book = booksList[index]

            booksList[index] = when (book) {
                is MultipleBooks -> {
                    book.copy(progress = progress)
                }

                is BookFile -> {
                    book.copy(progress = progress)
                }

                else -> {
                    book
                }
            }

            _uiState.tryEmit(BooksListUiState.BookListSuccess(booksList.toList()))
        }
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