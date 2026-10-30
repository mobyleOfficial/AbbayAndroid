package com.mobyle.abbay.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mobyle.abbay.data.database.entity.CharacterNoteEntity
import com.mobyle.abbay.data.database.entity.ListeningDayEntity
import com.mobyle.abbay.data.database.entity.QueueEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtrasDao {
    @Query("SELECT * FROM listening_day")
    fun observeDays(): Flow<List<ListeningDayEntity>>

    @Query("SELECT * FROM listening_day WHERE book_id = :bookId")
    fun observeBookDays(bookId: Long): Flow<List<ListeningDayEntity>>

    @Query("SELECT * FROM listening_day WHERE book_id = :bookId AND date = :date")
    suspend fun getDay(bookId: Long, date: String): ListeningDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(day: ListeningDayEntity)

    @Transaction
    suspend fun addToDay(day: ListeningDayEntity) {
        val current = getDay(day.bookId, day.date)
        upsertDay(
            current?.copy(
                bookTimeMs = current.bookTimeMs + day.bookTimeMs,
                wallTimeMs = current.wallTimeMs + day.wallTimeMs,
                savedBySpeedMs = current.savedBySpeedMs + day.savedBySpeedMs,
                savedBySilenceMs = current.savedBySilenceMs + day.savedBySilenceMs,
            ) ?: day,
        )
    }

    @Query("DELETE FROM listening_day")
    suspend fun clearDays()

    @Query("SELECT book_id FROM queue_entry ORDER BY position")
    fun observeQueue(): Flow<List<Long>>

    @Query("SELECT book_id FROM queue_entry ORDER BY position")
    suspend fun getQueue(): List<Long>

    @Query("DELETE FROM queue_entry")
    suspend fun clearQueue()

    @Insert
    suspend fun insertQueue(entries: List<QueueEntryEntity>)

    @Transaction
    suspend fun replaceQueue(bookIds: List<Long>) {
        clearQueue()
        insertQueue(bookIds.mapIndexed { index, bookId -> QueueEntryEntity(bookId, index) })
    }

    @Query(
        "SELECT * FROM character_note WHERE book_id = :bookId " +
            "ORDER BY first_position_ms IS NULL, first_position_ms, name COLLATE NOCASE",
    )
    fun observeCharacters(bookId: Long): Flow<List<CharacterNoteEntity>>

    @Query("SELECT * FROM character_note WHERE book_id = :bookId ORDER BY id")
    suspend fun getCharacters(bookId: Long): List<CharacterNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCharacter(character: CharacterNoteEntity): Long

    @Query("SELECT book_id FROM character_note WHERE id = :characterId")
    suspend fun bookOfCharacter(characterId: Long): Long?

    @Query("DELETE FROM character_note WHERE id = :characterId")
    suspend fun deleteCharacter(characterId: Long)

    @Query("DELETE FROM character_note WHERE book_id = :bookId")
    suspend fun deleteCharactersOf(bookId: Long)

    @Insert
    suspend fun insertCharacters(characters: List<CharacterNoteEntity>)

    @Transaction
    suspend fun replaceCharacters(bookId: Long, characters: List<CharacterNoteEntity>) {
        deleteCharactersOf(bookId)
        insertCharacters(characters)
    }
}
