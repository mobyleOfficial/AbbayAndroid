package com.mobyle.abbay.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diagnostics.DiagnosticsReport
import com.usecase.GetDiagnostics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val getDiagnostics: GetDiagnostics) : ViewModel() {
    private val mutableReport = MutableStateFlow<DiagnosticsReport?>(null)
    val report: StateFlow<DiagnosticsReport?> = mutableReport.asStateFlow()

    private val mutableIncludeFileNames = MutableStateFlow(false)
    val includeFileNames: StateFlow<Boolean> = mutableIncludeFileNames.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch { mutableReport.value = getDiagnostics() }

    fun setIncludeFileNames(include: Boolean) {
        mutableIncludeFileNames.value = include
    }
}
