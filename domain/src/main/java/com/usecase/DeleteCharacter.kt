package com.usecase

import com.repository.CharacterRepository
import javax.inject.Inject

class DeleteCharacter @Inject constructor(private val repository: CharacterRepository) {
    suspend operator fun invoke(characterId: Long) = repository.delete(characterId)
}
