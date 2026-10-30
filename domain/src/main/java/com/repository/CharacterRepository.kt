package com.repository

import com.model.CharacterNote
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observeCharacters(bookId: String): Flow<List<CharacterNote>>

    suspend fun save(character: CharacterNote)

    suspend fun delete(characterId: Long)
}
