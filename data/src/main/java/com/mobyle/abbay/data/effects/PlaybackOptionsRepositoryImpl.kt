package com.mobyle.abbay.data.effects

import com.mobyle.abbay.data.database.dao.AudioEffectsDao
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.sidecar.SidecarSync
import com.player.PlaybackOptions
import com.player.RepeatMode
import com.repository.PlaybackOptionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Repeat and skip options in `listening_state`; the sidecar carries them too. */
@Singleton
class PlaybackOptionsRepositoryImpl @Inject constructor(
    private val dao: AudioEffectsDao,
    private val libraryDao: LibraryDao,
    private val defaults: EffectDefaults,
    private val sidecarSync: SidecarSync,
) : PlaybackOptionsRepository {
    override fun observeOptions(bookId: String): Flow<PlaybackOptions> {
        val id = bookId.toLongOrNull() ?: return emptyFlow()
        return dao.observeListeningState(id).map { state -> state?.toOptions() ?: PlaybackOptions() }.distinctUntilChanged()
    }

    override suspend fun updateOptions(bookId: String, options: PlaybackOptions) = withContext(Dispatchers.IO) {
        val id = bookId.toLongOrNull() ?: return@withContext
        val current = libraryDao.getListeningState(id) ?: defaults.newListeningState(id)
        libraryDao.upsertListeningState(
            current.copy(
                repeatMode = options.repeatMode.name,
                loopAMs = options.loopAMs,
                loopBMs = options.loopBMs,
                skipIntroSeconds = options.skipIntroSeconds,
                skipOutroSeconds = options.skipOutroSeconds,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        sidecarSync.onBookChanged(id)
    }

    private fun ListeningStateEntity.toOptions() = PlaybackOptions(
        repeatMode = RepeatMode.entries.firstOrNull { it.name == repeatMode } ?: RepeatMode.OFF,
        loopAMs = loopAMs,
        loopBMs = loopBMs,
        skipIntroSeconds = skipIntroSeconds,
        skipOutroSeconds = skipOutroSeconds,
    )
}
