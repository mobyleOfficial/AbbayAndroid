package com.mobyle.abbay.presentation

import android.Manifest
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.mobyle.abbay.infra.common.BaseViewModel
import com.mobyle.abbay.presentation.utils.permissions.CheckPermissionsProvider
import com.model.Book
import com.usecase.GetCurrentSelectedBook
import com.usecase.ObserveBooksList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    observeBooksList: ObserveBooksList,
    val getCurrentSelectedBook: GetCurrentSelectedBook,
    checkPermissionsProvider: CheckPermissionsProvider,
) : BaseViewModel() {
    private val hasPermissions =
        MutableStateFlow(checkPermissionsProvider.areAllPermissionsGranted(getPermissionsList()))

    val selectedBook = MutableStateFlow<BookSelectionState>(BookSelectionState.Idle)

    init {
        observeBooksList()
            .take(1)
            .onEach {
                if (hasPermissions.value) {
                    it.firstOrNull {
                        it.id == getCurrentSelectedBook()
                    }?.let {
                        selectedBook.tryEmit(BookSelectionState.Selected(it))
                    } ?: kotlin.run {
                        selectedBook.tryEmit(BookSelectionState.None)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getPermissionsList() = if (Build.VERSION.SDK_INT >= 33) {
        PERMISSIONS_LIST
    } else {
        API_32_OR_LESS_PERMISSIONS_LIST
    }

    private companion object {
        val API_32_OR_LESS_PERMISSIONS_LIST = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val PERMISSIONS_LIST = listOf(Manifest.permission.READ_MEDIA_AUDIO)
    }
}

sealed class BookSelectionState {
    data object None : BookSelectionState()
    data class Selected(val book: Book) : BookSelectionState()
    data object Idle : BookSelectionState()
}
