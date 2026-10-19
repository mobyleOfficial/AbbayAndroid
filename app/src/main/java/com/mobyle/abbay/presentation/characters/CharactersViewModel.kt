package com.mobyle.abbay.presentation.characters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.CharacterNote
import com.usecase.DeleteCharacter
import com.usecase.ObserveCharacters
import com.usecase.SaveCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A book's notebook of characters. */
@HiltViewModel
class CharactersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCharacters: ObserveCharacters,
    private val saveCharacter: SaveCharacter,
    private val deleteCharacter: DeleteCharacter,
) : ViewModel() {
    val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    /** Null until the database answers. */
    val characters: StateFlow<List<CharacterNote>?> = observeCharacters(bookId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(character: CharacterNote) = viewModelScope.launch { saveCharacter(character.copy(bookId = bookId)) }

    fun delete(character: CharacterNote) = viewModelScope.launch { deleteCharacter(character.id) }

    /** Undo of a delete: the same character comes back as a new row. */
    fun restore(character: CharacterNote) = viewModelScope.launch { saveCharacter(character.copy(id = 0L, bookId = bookId)) }

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
    }
}
