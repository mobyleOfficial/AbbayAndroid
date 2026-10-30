package com.mobyle.abbay.fakes

import com.repository.StatisticsRepository
import com.statistics.ListeningDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory listening days. */
class FakeStatisticsRepository : StatisticsRepository {
    val days = MutableStateFlow(emptyList<ListeningDay>())

    override suspend fun record(day: ListeningDay) {
        days.value += day
    }

    override fun observeDays(): Flow<List<ListeningDay>> = days

    override fun observeBookDays(bookId: String): Flow<List<ListeningDay>> = days.map { all -> all.filter { it.bookId == bookId } }

    override suspend fun clear() {
        days.value = emptyList()
    }
}
