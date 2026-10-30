package com.mobyle.abbay.data.effects

import com.effects.AudioEffects
import com.effects.EqualizerPreset
import com.mobyle.abbay.data.database.dao.AudioEffectsDao
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.EqualizerPresetEntity
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.repository.AudioEffectsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectsRepositoryImpl @Inject constructor(
    private val dao: AudioEffectsDao,
    private val libraryDao: LibraryDao,
    private val defaults: EffectDefaults,
    private val sidecarSync: SidecarSync,
) : AudioEffectsRepository {
    override fun observeEffects(bookId: String): Flow<AudioEffects> {
        val id = bookId.toLongOrNull() ?: return emptyFlow()
        return dao.observeListeningState(id)
            .map { state -> state?.let(EffectsMapper::toEffects) ?: defaults.effects() }
            .distinctUntilChanged()
    }

    override suspend fun updateEffects(bookId: String, effects: AudioEffects) = withContext(Dispatchers.IO) {
        val id = bookId.toLongOrNull() ?: return@withContext
        val current = libraryDao.getListeningState(id) ?: defaults.newListeningState(id)
        libraryDao.upsertListeningState(EffectsMapper.withEffects(current, effects).copy(updatedAt = System.currentTimeMillis()))
        sidecarSync.onBookChanged(id)
    }

    override suspend fun applyEqualizerToAllBooks(gains: List<Int>) = withContext(Dispatchers.IO) {
        dao.setEqualizerOfAllBooks(EffectsMapper.jsonOf(gains), System.currentTimeMillis())
    }

    override fun observePresets(): Flow<List<EqualizerPreset>> = dao.observePresets().map { stored ->
        EqualizerPreset.BUILT_IN + stored.map { EqualizerPreset(it.id, it.name, EffectsMapper.gainsOf(it.gainsJson), isBuiltIn = false) }
    }

    override suspend fun savePreset(name: String, gains: List<Int>): Long = withContext(Dispatchers.IO) {
        dao.insertPreset(EqualizerPresetEntity(name = name, gainsJson = EffectsMapper.jsonOf(gains) ?: "[0,0,0,0,0,0,0,0,0,0]"))
    }

    override suspend fun deletePreset(presetId: Long) = withContext(Dispatchers.IO) {
        if (presetId > 0) dao.deletePreset(presetId)
    }

    override suspend fun addSilenceSaved(bookId: String, milliseconds: Long) = withContext(Dispatchers.IO) {
        bookId.toLongOrNull()?.let { dao.addSilenceSaved(it, milliseconds) }
        Unit
    }
}
