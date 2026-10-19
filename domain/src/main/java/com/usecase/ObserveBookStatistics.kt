package com.usecase

import com.repository.StatisticsRepository
import com.statistics.BookStatistics
import com.statistics.StatisticsAggregator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class ObserveBookStatistics @Inject constructor(private val repository: StatisticsRepository) {
    operator fun invoke(bookId: String, remainingBookTimeMs: Long, today: () -> LocalDate = LocalDate::now): Flow<BookStatistics> =
        repository.observeBookDays(bookId).map { days -> StatisticsAggregator.book(days, remainingBookTimeMs, today()) }
}
