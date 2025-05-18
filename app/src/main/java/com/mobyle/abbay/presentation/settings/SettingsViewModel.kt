package com.mobyle.abbay.presentation.settings

import android.util.Log
import com.mobyle.abbay.infra.common.BaseViewModel
import com.usecase.ClearBooks
import com.usecase.DisableOpenPlayerInStartup
import com.usecase.DisablePlayWhenAppIsClosed
import com.usecase.EnableOpenPlayerInStartup
import com.usecase.EnablePlayWhenAppIsClosed
import com.usecase.IsOpenPlayerInStartup
import com.usecase.IsPlayWhenAppIsClosedEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val enableOpenPlayerInStartup: EnableOpenPlayerInStartup,
    private val disableOpenPlayerInStartup: DisableOpenPlayerInStartup,
    private val enablePlayWhenAppIsClosed: EnablePlayWhenAppIsClosed,
    private val disablePlayWhenAppIsClosed: DisablePlayWhenAppIsClosed,
    private val clearBooksUseCase: ClearBooks,
    isPlayWhenAppIsClosedEnabled: IsPlayWhenAppIsClosedEnabled,
    isOpenPlayerInStartup: IsOpenPlayerInStartup,
) : BaseViewModel() {
    private val _shouldPlayWhenAppIsClosed = MutableStateFlow(isPlayWhenAppIsClosedEnabled())
    private val _shouldOpenPlayerInStartup = MutableStateFlow(isOpenPlayerInStartup())
    private val _showShowDeleteConfirmation = MutableStateFlow(false)

    val shouldPlayWhenAppIsClosed: StateFlow<Boolean> get() = _shouldPlayWhenAppIsClosed
    val shouldOpenPlayerInStartup: StateFlow<Boolean> get() = _shouldOpenPlayerInStartup
    val showShowDeleteConfirmation: StateFlow<Boolean> get() = _showShowDeleteConfirmation

    fun changePlayWhenAppIsClosed(shouldPlayWhenAppIsClosed: Boolean) {
        if (shouldPlayWhenAppIsClosed) {
            enablePlayWhenAppIsClosed()
        } else {
            disablePlayWhenAppIsClosed()
        }

        _shouldPlayWhenAppIsClosed.update { shouldPlayWhenAppIsClosed }
    }

    fun changeOpenPlayerInStartUp(shouldOpenPlayerInStartUp: Boolean) {
        if (shouldOpenPlayerInStartUp) {
            enableOpenPlayerInStartup()
        } else {
            disableOpenPlayerInStartup()
        }
        _shouldOpenPlayerInStartup.update { shouldOpenPlayerInStartUp }
    }

    fun clearBooks() {
        launch {
            clearBooksUseCase()
        }
    }

    fun dismissDeleteConfirmation() {
        _showShowDeleteConfirmation.value = false
    }

    fun showDeleteConfirmation() {
        _showShowDeleteConfirmation.value = true
    }
}