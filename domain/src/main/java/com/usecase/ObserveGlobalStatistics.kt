package com.usecase

import com.repository.StatisticsRepository
import com.statistics.GlobalStatistics
import com.statistics.StatisticsAggregator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class ObserveGlobalStatistics @Inject constructor(private val repository: StatisticsRepository) {
    operator fun invoke(today: () -> LocalDate = LocalDate::now): Flow<GlobalStatistics> =
        repository.observeDays().map { days -> StatisticsAggregator.global(days, today()) }
}
