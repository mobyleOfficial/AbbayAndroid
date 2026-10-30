package com.repository

import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    fun observeQueue(): Flow<List<String>>

    suspend fun add(bookId: String, playNext: Boolean)

    suspend fun remove(bookId: String)

    suspend fun move(bookId: String, index: Int)

    suspend fun takeNext(): String?
}
