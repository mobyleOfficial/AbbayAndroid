package com.usecase

import com.repository.StatisticsRepository
import javax.inject.Inject

class ClearStatistics @Inject constructor(private val repository: StatisticsRepository) {
    suspend operator fun invoke() = repository.clear()
}
