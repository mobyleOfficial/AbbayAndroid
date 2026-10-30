package com.mobyle.abbay.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ChapterEntity
import com.mobyle.abbay.data.database.entity.LibraryRootEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_root ORDER BY added_at")
    fun observeRoots(): Flow<List<LibraryRootEntity>>

    @Query("SELECT * FROM library_root ORDER BY added_at")
    suspend fun getRoots(): List<LibraryRootEntity>

    @Query("SELECT * FROM library_root WHERE tree_uri = :treeUri")
    suspend fun getRootByUri(treeUri: String): LibraryRootEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoot(root: LibraryRootEntity): Long

    @Query("UPDATE library_root SET is_accessible = :isAccessible WHERE id = :rootId")
    suspend fun setRootAccessible(rootId: Long, isAccessible: Boolean)

    @Query("DELETE FROM library_root WHERE id = :rootId")
    suspend fun deleteRoot(rootId: Long)

    @Transaction
    @Query("SELECT * FROM book WHERE hidden = 0")
    fun observeVisibleBooks(): Flow<List<BookWithDetails>>

    @Query("SELECT * FROM book WHERE root_id = :rootId")
    suspend fun getBooksOfRoot(rootId: Long): List<BookEntity>

    @Query("SELECT * FROM book WHERE root_id IS NULL")
    suspend fun getSingleFileBooks(): List<BookEntity>

    @Transaction
    @Query("SELECT * FROM book")
    suspend fun getAllBooksWithDetails(): List<BookWithDetails>

    @Query("SELECT * FROM book WHERE id = :bookId")
    suspend fun getBook(bookId: Long): BookEntity?

    @Transaction
    @Query("SELECT * FROM book WHERE id = :bookId")
    fun observeBookWithDetails(bookId: Long): Flow<BookWithDetails?>

    @Query(
        "UPDATE book SET title_override = :title, author_override = :author, narrator_override = :narrator, " +
            "description_override = :description WHERE id = :bookId",
    )
    suspend fun updateOverrides(bookId: Long, title: String?, author: String?, narrator: String?, description: String?)

    @Query("UPDATE book SET cover_uri = :coverUri, cover_source = :coverSource WHERE id = :bookId")
    suspend fun updateCover(bookId: Long, coverUri: String?, coverSource: String?)

    @Transaction
    @Query("SELECT * FROM book WHERE id = :bookId")
    suspend fun getBookWithDetails(bookId: Long): BookWithDetails?

    @Query("SELECT COUNT(*) FROM book")
    suspend fun countBooks(): Int

    @Query("SELECT * FROM book WHERE missing_since IS NOT NULL AND missing_since < :limit")
    suspend fun getBooksMissingSince(limit: Long): List<BookEntity>

    @Query("SELECT * FROM book")
    suspend fun getAllBooks(): List<BookEntity>

    @Insert
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE book SET hidden = 1 WHERE id = :bookId")
    suspend fun hideBook(bookId: Long)

    @Query("UPDATE book SET missing_since = :missingSince WHERE id = :bookId")
    suspend fun setMissingSince(bookId: Long, missingSince: Long?)

    @Query("DELETE FROM book WHERE id IN (:bookIds)")
    suspend fun deleteBooks(bookIds: List<Long>)

    @Query("DELETE FROM book")
    suspend fun deleteAllBooks()

    @Query("DELETE FROM library_root")
    suspend fun deleteAllRoots()

    @Query("SELECT * FROM track WHERE book_id = :bookId ORDER BY index_in_book")
    suspend fun getTracks(bookId: Long): List<TrackEntity>

    @Query("DELETE FROM track WHERE book_id = :bookId")
    suspend fun deleteTracks(bookId: Long)

    @Insert
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Transaction
    suspend fun replaceTracks(bookId: Long, tracks: List<TrackEntity>) {
        deleteTracks(bookId)
        insertTracks(tracks)
    }

    @Query("DELETE FROM chapter WHERE book_id = :bookId")
    suspend fun deleteChapters(bookId: Long)

    @Insert
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Transaction
    suspend fun replaceChapters(bookId: Long, chapters: List<ChapterEntity>) {
        deleteChapters(bookId)
        insertChapters(chapters)
    }

    @Query("SELECT * FROM book WHERE sidecar_id = :sidecarId")
    suspend fun getBookBySidecarId(sidecarId: String): BookEntity?

    @Query("SELECT * FROM book WHERE relative_path = :relativePath")
    suspend fun getBooksByRelativePath(relativePath: String): List<BookEntity>

    @Query("UPDATE book SET sidecar_id = :sidecarId WHERE id = :bookId")
    suspend fun updateSidecarId(bookId: Long, sidecarId: String)

    @Query("SELECT * FROM listening_state WHERE book_id = :bookId")
    suspend fun getListeningState(bookId: Long): ListeningStateEntity?

    @Upsert
    suspend fun upsertListeningState(state: ListeningStateEntity)

    @Transaction
    suspend fun clearLibrary() {
        deleteAllBooks()
        deleteAllRoots()
    }
}
