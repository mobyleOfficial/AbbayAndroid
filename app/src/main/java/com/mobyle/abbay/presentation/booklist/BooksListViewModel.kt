package com.mobyle.abbay.presentation.booklist

import android.Manifest
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.mobyle.abbay.infra.common.BaseViewModel
import com.mobyle.abbay.presentation.utils.permissions.CheckPermissionsProvider
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.usecase.DeleteBook
import com.usecase.ForceUpdateList
import com.usecase.GetBooksFolderPath
import com.usecase.GetBooksList
import com.usecase.GetCurrentSelectedBook
import com.usecase.IsOpenPlayerInStartup
import com.usecase.SaveBookFolderPath
import com.usecase.SaveCurrentSelectedBook
import com.usecase.UpsertBookList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val getBooksList: GetBooksList,
    private val upsertBookList: UpsertBookList,
    private val deleteBook: DeleteBook,
    private val saveCurrentSelectedBookUC: SaveCurrentSelectedBook,
    val getBooksFolderPath: GetBooksFolderPath,
    val saveBookFolderPath: SaveBookFolderPath,
    val getCurrentSelectedBook: GetCurrentSelectedBook,
    val isOpenPlayerInStartupUC: IsOpenPlayerInStartup,
    val checkPermissionsProvider: CheckPermissionsProvider,
    forceUpdateList: ForceUpdateList,
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

    private val _hasSelectedFolder = MutableStateFlow(getBooksFolderPath() != null)
    val hasSelectedFolder: StateFlow<Boolean> get() = _hasSelectedFolder

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing

    init {
        if (checkPermissionsProvider.areAllPermissionsGranted(getPermissionsList())) {
            getAudiobookList()
        } else {
            _uiState.value = BooksListUiState.NoPermissionsGranted
        }

        forceUpdateList()
            .onEach {
                booksList.clear()
                booksIdList.value = emptyList()
                _selectedBook.value = null
                getAudiobookList()
            }
            .launchIn(viewModelScope)
    }

    fun getPermissionsList() = if (Build.VERSION.SDK_INT >= 33) {
        PERMISSIONS_LIST
    } else {
        API_32_OR_LESS_PERMISSIONS_LIST
    }

    fun updateBookList(booksList: List<Book>) = launch {
        this.booksList.clear()
        this.booksList.addAll(booksList)
        upsertBookList.invoke(booksList)
        val newBookList = mutableListOf<Book>()
        newBookList.addAll(this.booksList)
        _uiState.emit(BooksListUiState.BookListSuccess(newBookList))
    }

    fun updateSelectedBook(book: Book) {
        selectBook(book)
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
        _hasSelectedFolder.value = true
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
        saveCurrentSelectedBookUC(book?.id)
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
    fun updateBookSpeed(id: String, speed: Float) {
        booksList.firstOrNull { it.id == id }?.let {
            val newBook = when (it) {
                is BookFile -> {
                    it.copy(speed = speed)
                }

                is MultipleBooks -> {
                    it.copy(speed = speed)
                }

                else -> {null}
            }

            newBook?.let { book ->
                booksList[booksList.indexOf(it)] = book
                selectBook(book)
            }
        }

        launch {
            upsertBookList.invoke(booksList)
            _uiState.tryEmit(BooksListUiState.BookListSuccess(booksList.toList()))
        }
    }

    fun getAudiobookList() = launch {
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

    fun removeBook(book: Book) {
        viewModelScope.launch {
            booksList.removeIf {
                it.id == book.id
            }

            val updatedList = booksList
            _uiState.value = BooksListUiState.BookListSuccess(updatedList)

            deleteBook.invoke(book)

            if (selectedBook.value?.id == book.id) {
                _selectedBook.value = null
            }
        }
    }

    fun markBookAsError(book: Book) = launch {
        val index = booksList.indexOfFirst { it.id == book.id }
        if (index != -1) {
            val updatedBook = when (book) {
                is BookFile -> book.copy(hasError = true)
                is MultipleBooks -> book.copy(hasError = true)
                else -> book
            }
            booksList[index] = updatedBook
            _uiState.emit(BooksListUiState.BookListSuccess(booksList.toList()))
        }
    }

    fun checkForNewBooks(newBooksList: List<Book>) {
        val currentIds = booksList.map { it.id }.toSet()
        val newBooks = newBooksList.filter { book ->
            !currentIds.contains(book.id)
        }
        
        if (newBooks.isNotEmpty()) {
            addAllBookTypes(newBooks)
        }
        _isRefreshing.value = false
    }

    fun setRefreshingLoading() {
        _isRefreshing.value = true
    }

    fun hasPermissions() = checkPermissionsProvider.areAllPermissionsGranted(getPermissionsList())

    sealed class BooksListUiState {
        data class BookListSuccess(val audiobookList: List<Book>) :
            BooksListUiState()

        data object NoBookSelected : BooksListUiState()
        data object GenericError : BooksListUiState()
        data object Loading : BooksListUiState()

        data object NoPermissionsGranted : BooksListUiState()
    }

    companion object {
        val API_32_OR_LESS_PERMISSIONS_LIST = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val PERMISSIONS_LIST = listOf(Manifest.permission.READ_MEDIA_AUDIO)

    }
}