package com.mobyle.abbay.presentation.booklist

import com.mobyle.abbay.infra.common.BaseViewModel
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.usecase.GetBooksList
import com.usecase.IsOpenPlayerInStartup
import com.usecase.IsPlayWhenAppIsClosedEnabled
import com.usecase.UpsertBookList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val getBooksList: GetBooksList,
    private val upsertBookList: UpsertBookList,
    val isOpenPlayerInStartupUC: IsOpenPlayerInStartup,
    isPlayWhenAppIsClosedEnabledUC: IsPlayWhenAppIsClosedEnabled,

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

    val booksIdList = MutableStateFlow<List<Book?>>(emptyList())

    var shouldOpenPlayerInStartup = false

    init {
        getAudiobookList()
    }

    fun updateBookList(booksList: List<Book>) = launch {
        this.booksList.addAll(booksList)
        upsertBookList.invoke(booksList)
        val newBookList = mutableListOf<Book>()
        newBookList.addAll(this.booksList)
        _uiState.emit(BooksListUiState.BookListSuccess(newBookList))
    }

    fun addThumbnails(booksWithThumbList: List<Book>) = launch {
        val newList = this.booksList.map { book ->
            booksWithThumbList.firstOrNull { it.id == book.id }?.let {
                when (it) {
                    is MultipleBooks -> it.copy(thumbnail = it.thumbnail)
                    is BookFile -> it.copy(thumbnail = it.thumbnail)
                    else -> book
                }
            } ?: book
        }.toList()

        upsertBookList.invoke(newList)
        this.booksList.clear()
        this.booksList.addAll(newList)

        _uiState.emit(BooksListUiState.BookListSuccess(this.booksList.toList()))
    }

    fun addAllBookTypes(filesList: List<Book>) = launch {
        this.booksList.addAll(filesList)
        upsertBookList.invoke(booksList)
        val newBookList = mutableListOf<Book>()
        newBookList.addAll(this.booksList)
        _uiState.emit(BooksListUiState.BookListSuccess(newBookList))
        booksIdList.emit(this.booksList)
    }

    fun updateBookProgress(id: String, progress: Long) {
        booksList.firstOrNull { it.id == id }?.let {

            when (it) {
                is BookFile -> {
                    booksList[booksList.indexOf(it)] = it.copy(progress = progress)
                }

                is MultipleBooks -> {
                    booksList[booksList.indexOf(it)] = it.copy(progress = progress)
                }

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

            val mappedBook = when (val book = booksList[index]) {
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
            booksList[index] = mappedBook
            _uiState.tryEmit(BooksListUiState.BookListSuccess(booksList.toList()))
            selectBook(mappedBook)

        }
    }

    fun updateBookPosition(id: String, position: Int) {
        val index = booksList.indexOfFirst { it.id == id }

        if (index != -1) {
            val book = booksList[index]

            if (book is MultipleBooks) {
                booksList[index] = book.copy(currentBookPosition = position)

                _uiState.tryEmit(BooksListUiState.BookListSuccess(booksList.toList()))
                selectBook(book.copy(currentBookPosition = position))
            }
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

    fun showLoading() {
        _uiState.value = BooksListUiState.Loading
    }

    fun shouldOpenPlayerInStartup() {
        shouldOpenPlayerInStartup = isOpenPlayerInStartupUC()
    }

    sealed class BooksListUiState {
        data class BookListSuccess(val audiobookList: List<Book>) :
            BooksListUiState()

        data object NoBookSelected : BooksListUiState()
        data object GenericError : BooksListUiState()
        data object Loading : BooksListUiState()
    }
}