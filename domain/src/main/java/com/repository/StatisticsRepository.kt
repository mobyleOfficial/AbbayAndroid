package com.repository

import com.statistics.ListeningDay
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    suspend fun record(day: ListeningDay)

    fun observeDays(): Flow<List<ListeningDay>>

    fun observeBookDays(bookId: String): Flow<List<ListeningDay>>

    suspend fun clear()
}
