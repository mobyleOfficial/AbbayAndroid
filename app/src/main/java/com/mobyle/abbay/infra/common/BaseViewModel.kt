package com.mobyle.abbay.infra.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseViewModel(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    fun launch(block: suspend () -> Unit) = viewModelScope.launch(dispatcher) {
        block()
    }
}