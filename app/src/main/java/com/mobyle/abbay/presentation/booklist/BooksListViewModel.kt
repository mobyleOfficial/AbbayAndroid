package com.mobyle.abbay.presentation.booklist

import android.Manifest
import android.os.Build
import android.util.Log
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
import com.usecase.HasShownReloadGuide
import com.usecase.IsOpenPlayerInStartup
import com.usecase.ObserveBooksList
import com.usecase.SaveBookFolderPath
import com.usecase.SaveCurrentSelectedBook
import com.usecase.SetReloadGuideAsShown
import com.usecase.UpsertBookList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksListViewModel @Inject constructor(
    //private val getBooksList: GetBooksList,
    private val observeBooksList: ObserveBooksList,
    private val upsertBookList: UpsertBookList,
    private val deleteBook: DeleteBook,
    private val saveCurrentSelectedBookUC: SaveCurrentSelectedBook,
    private val setReloadGuideAsShown: SetReloadGuideAsShown,
    val hasShownReloadGuide: HasShownReloadGuide,
    val getBooksFolderPath: GetBooksFolderPath,
    val saveBookFolderPath: SaveBookFolderPath,
    val getCurrentSelectedBook: GetCurrentSelectedBook,
    val isOpenPlayerInStartupUC: IsOpenPlayerInStartup,
    checkPermissionsProvider: CheckPermissionsProvider,
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

    var shouldOpenPlayerInStartup = false

    private val _hasSelectedFolder = MutableStateFlow(getBooksFolderPath() != null)
    val hasSelectedFolder: StateFlow<Boolean> get() = _hasSelectedFolder

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing

    private val _showReloadGuide = MutableStateFlow(false)
    val showReloadGuide: StateFlow<Boolean> get() = _showReloadGuide

    private val hasPermissions =
        MutableStateFlow(checkPermissionsProvider.areAllPermissionsGranted(getPermissionsList()))

    init {
        combine(
            observeBooksList(), hasPermissions
        ) { domainBookList, hasPermissions ->
            this.booksList.clear()
            this.booksList.addAll(domainBookList)

            val state = if (hasPermissions) {
                if (domainBookList.isEmpty()) {
                    BooksListUiState.NoBookSelected
                } else {
                    BooksListUiState.BookListSuccess(domainBookList)
                }
            } else {
                BooksListUiState.NoPermissionsGranted
            }

            _uiState.tryEmit(state)
        }
            .launchIn(viewModelScope)
    }

    fun setUserHasPermissions() {
        hasPermissions.value = true
    }

    fun getPermissionsList() = if (Build.VERSION.SDK_INT >= 33) {
        PERMISSIONS_LIST
    } else {
        API_32_OR_LESS_PERMISSIONS_LIST
    }

    fun updateBookList(booksList: List<Book>) = launch {
        upsertBookList(booksList)
    }

    fun updateSelectedBook(book: Book) {
        selectBook(book)
    }

    fun addAllBookTypes(filesList: List<Book>) = launch {
        upsertBookList(filesList)
        _hasSelectedFolder.value = true
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

    fun updateBookList() = launch {
        upsertBookList(booksList)
    }

    fun selectBook(book: Book?) {
        saveCurrentSelectedBookUC(book?.id)
        _selectedBook.tryEmit(book)
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

                else -> {
                    null
                }
            }

            newBook?.let { book ->
                booksList[booksList.indexOf(it)] = book
                selectBook(book)
            }
        }

//        launch {
//            upsertBookList.invoke(booksList)
//        }
    }

    fun showLoading() {
        _uiState.value = BooksListUiState.Loading
    }

    fun shouldOpenPlayerInStartup() {
        shouldOpenPlayerInStartup = isOpenPlayerInStartupUC()
    }

    fun removeBook(book: Book) {
        viewModelScope.launch {
            deleteBook.invoke(book)

            if (selectedBook.value?.id == book.id) {
                _selectedBook.value = null
                saveCurrentSelectedBookUC(null)
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

//            launch {
//                upsertBookList(booksList)
//            }
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

    fun showReloadGuide() {
        if (!hasShownReloadGuide()) {
            _showReloadGuide.value = true
        }
    }

    fun dismissReloadGuide() {
        setReloadGuideAsShown()
        _showReloadGuide.value = false
    }

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