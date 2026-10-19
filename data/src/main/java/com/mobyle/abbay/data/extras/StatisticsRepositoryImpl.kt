package com.mobyle.abbay.data.extras

import com.mobyle.abbay.data.database.dao.ExtrasDao
import com.mobyle.abbay.data.database.entity.ListeningDayEntity
import com.repository.StatisticsRepository
import com.statistics.ListeningDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(private val dao: ExtrasDao) : StatisticsRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun record(day: ListeningDay) = withContext(ioDispatcher) {
        val bookId = day.bookId.toLongOrNull() ?: return@withContext
        dao.addToDay(
            ListeningDayEntity(
                bookId = bookId,
                date = day.date.toString(),
                bookTimeMs = day.bookTimeMs,
                wallTimeMs = day.wallTimeMs,
                savedBySpeedMs = day.savedBySpeedMs,
                savedBySilenceMs = day.savedBySilenceMs,
            ),
        )
    }

    override fun observeDays(): Flow<List<ListeningDay>> = dao.observeDays().map { days -> days.mapNotNull { it.toDomain() } }

    override fun observeBookDays(bookId: String): Flow<List<ListeningDay>> {
        val id = bookId.toLongOrNull() ?: return emptyFlow()
        return dao.observeBookDays(id).map { days -> days.mapNotNull { it.toDomain() } }
    }

    override suspend fun clear() = withContext(ioDispatcher) { dao.clearDays() }

    private fun ListeningDayEntity.toDomain(): ListeningDay? {
        val localDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        return ListeningDay(bookId.toString(), localDate, bookTimeMs, wallTimeMs, savedBySpeedMs, savedBySilenceMs)
    }
}
