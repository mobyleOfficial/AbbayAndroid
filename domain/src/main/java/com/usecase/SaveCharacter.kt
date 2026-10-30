package com.usecase

import com.model.CharacterNote
import com.repository.CharacterRepository
import javax.inject.Inject

class SaveCharacter @Inject constructor(private val repository: CharacterRepository) {
    suspend operator fun invoke(character: CharacterNote) {
        if (character.name.isBlank()) return
        repository.save(character.copy(name = character.name.trim(), description = character.description.trim()))
    }
}
