package com.mobyle.abbay.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.CompanionDocument
import com.usecase.GetCompanionDocuments
import com.usecase.RememberOpenedDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanionDocumentsViewModel @Inject constructor(
    private val getCompanionDocuments: GetCompanionDocuments,
    private val rememberOpenedDocument: RememberOpenedDocument,
) : ViewModel() {
    private val mutableBookId = MutableStateFlow<String?>(null)
    val bookId: StateFlow<String?> = mutableBookId.asStateFlow()

    private val mutableDocuments = MutableStateFlow<List<CompanionDocument>?>(null)
    val documents: StateFlow<List<CompanionDocument>?> = mutableDocuments.asStateFlow()

    fun show(bookId: String) {
        mutableBookId.value = bookId
        mutableDocuments.value = null
        viewModelScope.launch { mutableDocuments.value = getCompanionDocuments(bookId) }
    }

    fun dismiss() {
        mutableBookId.value = null
    }

    fun opened(document: CompanionDocument) {
        val id = mutableBookId.value ?: return
        viewModelScope.launch { rememberOpenedDocument(id, document.uri) }
        dismiss()
    }
}
