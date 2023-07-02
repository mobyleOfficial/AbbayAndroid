package com.mobyle.abbay.presentation.booklist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionHandlerViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow("")
    val uiState: StateFlow<String> get() = _uiState

    fun getAudiobookList() {}

    sealed class PermissionHandlerUiState {
        data class AudiobookListSuccess(val audiobookList: List<String>) : PermissionHandlerUiState()
        object NoAudiobookSelected : PermissionHandlerUiState()
        object GenericError : PermissionHandlerUiState()
    }
}