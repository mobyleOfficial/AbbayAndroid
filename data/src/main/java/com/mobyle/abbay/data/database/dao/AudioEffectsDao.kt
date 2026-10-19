package com.mobyle.abbay.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mobyle.abbay.data.database.entity.EqualizerPresetEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import kotlinx.coroutines.flow.Flow

/** Effect columns of `listening_state` and the user equalizer presets. */
@Dao
interface AudioEffectsDao {
    @Query("SELECT * FROM listening_state WHERE book_id = :bookId")
    fun observeListeningState(bookId: Long): Flow<ListeningStateEntity?>

    @Query("UPDATE listening_state SET equalizer_json = :gainsJson, updated_at = :now")
    suspend fun setEqualizerOfAllBooks(gainsJson: String?, now: Long)

    @Query("UPDATE listening_state SET silence_saved_ms = silence_saved_ms + :milliseconds WHERE book_id = :bookId")
    suspend fun addSilenceSaved(bookId: Long, milliseconds: Long)

    @Query("SELECT * FROM equalizer_preset ORDER BY name COLLATE NOCASE")
    fun observePresets(): Flow<List<EqualizerPresetEntity>>

    @Insert
    suspend fun insertPreset(preset: EqualizerPresetEntity): Long

    @Query("DELETE FROM equalizer_preset WHERE id = :presetId")
    suspend fun deletePreset(presetId: Long)
}
