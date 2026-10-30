package com.mobyle.abbay.presentation.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.library.FolderLevel
import com.library.FolderLocation
import com.library.FolderTree
import com.model.LibraryRoot
import com.usecase.ObserveBooks
import com.usecase.ObserveLibraryRoots
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBooks: ObserveBooks,
    observeLibraryRoots: ObserveLibraryRoots,
) : ViewModel() {
    private val mutableLocation = MutableStateFlow(initialLocation(savedStateHandle))
    val location: StateFlow<FolderLocation> = mutableLocation.asStateFlow()

    val roots: StateFlow<List<LibraryRoot>> = observeLibraryRoots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val level: StateFlow<FolderLevel?> = combine(roots, observeBooks(), mutableLocation) { roots, books, location ->
        FolderTree.level(roots, books, location)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun open(location: FolderLocation) {
        mutableLocation.value = location
    }

    fun goUp(): Boolean {
        val current = mutableLocation.value
        mutableLocation.value = when {
            current.isTop -> return false
            current.segments.isNotEmpty() -> current.upTo(current.segments.size - 1)
            else -> FolderLocation.TOP
        }
        return true
    }

    private fun initialLocation(savedStateHandle: SavedStateHandle): FolderLocation {
        val rootId = savedStateHandle.get<String>(ROOT_ARGUMENT)?.toLongOrNull()
        val path = savedStateHandle.get<String>(PATH_ARGUMENT).orEmpty()
        return when (savedStateHandle.get<String>(KIND_ARGUMENT)) {
            KIND_SINGLE_FILES -> FolderLocation(isSingleFiles = true)
            KIND_FOLDER -> FolderLocation(rootId, path)
            else -> FolderLocation.TOP
        }
    }

    companion object {
        const val KIND_ARGUMENT = "kind"
        const val ROOT_ARGUMENT = "rootId"
        const val PATH_ARGUMENT = "path"
        const val KIND_TOP = "top"
        const val KIND_FOLDER = "folder"
        const val KIND_SINGLE_FILES = "single"
    }
}
