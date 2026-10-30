package com.repository

import kotlinx.coroutines.flow.Flow

/** The queue of books to play next. */
interface QueueRepository {
    /** Book ids in queue order. */
    fun observeQueue(): Flow<List<String>>

    /** "Play next" puts the book first; "Add to queue" last. A book already queued moves. */
    suspend fun add(bookId: String, playNext: Boolean)

    suspend fun remove(bookId: String)

    /** Moves [bookId] to [index] in the queue (drag to reorder). */
    suspend fun move(bookId: String, index: Int)

    /** Removes and returns the first book of the queue. */
    suspend fun takeNext(): String?
}
