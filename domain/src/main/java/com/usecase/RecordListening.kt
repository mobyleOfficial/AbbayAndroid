package com.usecase

import com.repository.SettingsRepository
import com.repository.StatisticsRepository
import com.statistics.ListeningDay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Adds listening time to the statistics, unless they are turned off. */
class RecordListening @Inject constructor(
    private val repository: StatisticsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(day: ListeningDay) {
        if (day.wallTimeMs <= 0L && day.savedBySilenceMs <= 0L) return
        if (settingsRepository.observeSettings().first().statisticsEnabled) repository.record(day)
    }
}
