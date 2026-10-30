package com.repository

import com.statistics.ListeningDay
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    /** Adds [day]'s times to what that book already has on that date. */
    suspend fun record(day: ListeningDay)

    fun observeDays(): Flow<List<ListeningDay>>

    fun observeBookDays(bookId: String): Flow<List<ListeningDay>>

    suspend fun clear()
}
