package com.repository

import com.model.CharacterNote
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    /** Characters of [bookId] by first appearance, then name. */
    fun observeCharacters(bookId: String): Flow<List<CharacterNote>>

    /** Adds ([CharacterNote.id] 0) or updates a character. */
    suspend fun save(character: CharacterNote)

    suspend fun delete(characterId: Long)
}
