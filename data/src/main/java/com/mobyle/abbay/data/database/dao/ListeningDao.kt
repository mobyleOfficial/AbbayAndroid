package com.mobyle.abbay.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mobyle.abbay.data.database.entity.BookmarkEntity
import com.mobyle.abbay.data.database.entity.HistoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningDao {
    @Query("SELECT * FROM bookmark WHERE book_id = :bookId ORDER BY book_position_ms, created_at")
    fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmark WHERE id = :bookmarkId")
    suspend fun getBookmark(bookmarkId: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmark WHERE book_id = :bookId ORDER BY book_position_ms, created_at")
    suspend fun getBookmarks(bookId: Long): List<BookmarkEntity>

    @Query("DELETE FROM bookmark WHERE book_id = :bookId")
    suspend fun deleteBookmarksOf(bookId: Long)

    @Insert
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Transaction
    suspend fun replaceBookmarks(bookId: Long, bookmarks: List<BookmarkEntity>) {
        deleteBookmarksOf(bookId)
        insertBookmarks(bookmarks)
    }

    @Query("SELECT * FROM history_event WHERE book_id = :bookId ORDER BY at DESC, id DESC")
    suspend fun getHistory(bookId: Long): List<HistoryEventEntity>

    @Query("UPDATE bookmark SET note = :note WHERE id = :bookmarkId")
    suspend fun updateBookmarkNote(bookmarkId: Long, note: String)

    @Query("DELETE FROM bookmark WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)

    @Query("SELECT * FROM history_event WHERE book_id = :bookId ORDER BY at DESC, id DESC")
    fun observeHistory(bookId: Long): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE book_id = :bookId ORDER BY at DESC, id DESC LIMIT 1")
    suspend fun getLatestHistoryEvent(bookId: Long): HistoryEventEntity?

    @Insert
    suspend fun insertHistoryEvent(event: HistoryEventEntity): Long

    @Update
    suspend fun updateHistoryEvent(event: HistoryEventEntity)

    @Query(
        "DELETE FROM history_event WHERE book_id = :bookId AND id NOT IN " +
            "(SELECT id FROM history_event WHERE book_id = :bookId ORDER BY at DESC, id DESC LIMIT :keep)",
    )
    suspend fun trimHistory(bookId: Long, keep: Int)

    @Transaction
    suspend fun addHistoryEvent(event: HistoryEventEntity, keep: Int) {
        insertHistoryEvent(event)
        trimHistory(event.bookId, keep)
    }
}
