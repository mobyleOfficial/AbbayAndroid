package com.mobyle.abbay.data.extras

import com.mobyle.abbay.data.database.dao.ExtrasDao
import com.mobyle.abbay.data.database.entity.CharacterNoteEntity
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.model.CharacterNote
import com.repository.CharacterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Character changes are book edits: they move the state's `updatedAt` and rewrite the sidecar. */
@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val dao: ExtrasDao,
    private val sidecarSync: SidecarSync,
) : CharacterRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun observeCharacters(bookId: String): Flow<List<CharacterNote>> {
        val id = bookId.toLongOrNull() ?: return emptyFlow()
        return dao.observeCharacters(id).map { characters ->
            characters.map { CharacterNote(it.id, it.bookId.toString(), it.name, it.description, it.firstPositionMs) }
        }
    }

    override suspend fun save(character: CharacterNote) = withContext(ioDispatcher) {
        val bookId = character.bookId.toLongOrNull() ?: return@withContext
        val name = character.name.trim().take(CharacterNote.MAX_NAME_LENGTH)
        if (name.isEmpty()) return@withContext
        dao.saveCharacter(
            CharacterNoteEntity(
                id = character.id,
                bookId = bookId,
                name = name,
                description = character.description.trim().take(CharacterNote.MAX_DESCRIPTION_LENGTH),
                firstPositionMs = character.firstPositionMs?.coerceAtLeast(0L),
            ),
        )
        sidecarSync.onBookEdited(bookId)
    }

    override suspend fun delete(characterId: Long) = withContext(ioDispatcher) {
        val bookId = dao.bookOfCharacter(characterId) ?: return@withContext
        dao.deleteCharacter(characterId)
        sidecarSync.onBookEdited(bookId)
    }
}
