package com.repository

import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfSignIn
import com.model.DataResult
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import kotlinx.coroutines.flow.Flow

/**
 * Books on the user's own Audiobookshelf server. Once added they are library books like LibriVox ones: their
 * downloads are started, paused and removed through [LibriVoxRepository].
 */
interface AudiobookshelfRepository {
    fun observeAccount(): Flow<AudiobookshelfAccount?>

    suspend fun signIn(serverUrl: String, username: String, password: String): AudiobookshelfSignIn

    suspend fun signOut()

    suspend fun libraries(): DataResult<List<AudiobookshelfLibrary>>

    suspend fun recentlyAdded(libraryId: String): DataResult<List<LibriVoxBook>>

    suspend fun search(libraryId: String, query: String): DataResult<List<LibriVoxBook>>

    fun coverUrlOf(itemId: String): String

    fun details(book: LibriVoxBook): Flow<DataResult<LibriVoxDetails>>

    /** Audiobookshelf item id to library book id. */
    fun observeLibraryBookIds(): Flow<Map<String, String>>

    suspend fun add(book: LibriVoxBook, allowsCellular: Boolean, download: Boolean = true): DataResult<String>
}
