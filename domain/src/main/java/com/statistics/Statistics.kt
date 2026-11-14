package com.statistics

import java.time.LocalDate
import java.time.YearMonth

data class PeriodTotal(val start: LocalDate, val wallTimeMs: Long)

data class BookTotal(val bookId: String, val wallTimeMs: Long)

data class GlobalStatistics(
    val totalWallTimeMs: Long,
    val totalBookTimeMs: Long,
    val last7Days: List<PeriodTotal>,
    val last30Days: List<PeriodTotal>,
    val last12Months: List<PeriodTotal>,
    val streakDays: Int,
    val topBooks: List<BookTotal>,
    val savedBySpeedMs: Long,
    val savedBySilenceMs: Long,
)

data class BookStatistics(
    val sessions: Int,
    val totalWallTimeMs: Long,
    val averageSpeed: Float,
    val estimatedFinish: LocalDate?,
)

object StatisticsAggregator {
    const val STREAK_MINIMUM_MS = 5 * 60_000L
    const val PACE_DAYS = 14L
    const val TOP_BOOKS = 5

    fun global(days: List<ListeningDay>, today: LocalDate): GlobalStatistics {
        val byDate = days.groupBy { it.date }.mapValues { (_, entries) -> entries.sumOf { it.wallTimeMs } }
        return GlobalStatistics(
            totalWallTimeMs = days.sumOf { it.wallTimeMs },
            totalBookTimeMs = days.sumOf { it.bookTimeMs },
            last7Days = dailyBars(byDate, today, WEEK),
            last30Days = dailyBars(byDate, today, MONTH),
            last12Months = monthlyBars(days, today),
            streakDays = streak(byDate, today),
            topBooks = days.groupBy { it.bookId }
                .map { (bookId, entries) -> BookTotal(bookId, entries.sumOf { it.wallTimeMs }) }
                .sortedByDescending { it.wallTimeMs }
                .take(TOP_BOOKS),
            savedBySpeedMs = days.sumOf { it.savedBySpeedMs },
            savedBySilenceMs = days.sumOf { it.savedBySilenceMs },
        )
    }

    fun book(days: List<ListeningDay>, remainingBookTimeMs: Long, today: LocalDate): BookStatistics {
        val wall = days.sumOf { it.wallTimeMs }
        val bookTime = days.sumOf { it.bookTimeMs }
        val recentBookTime = days.filter { !it.date.isBefore(today.minusDays(PACE_DAYS - 1)) }.sumOf { it.bookTimeMs }
        val dailyPace = recentBookTime / PACE_DAYS
        val estimate = if (remainingBookTimeMs > 0 && dailyPace > 0) {
            today.plusDays((remainingBookTimeMs + dailyPace - 1) / dailyPace)
        } else {
            null
        }
        return BookStatistics(
            sessions = days.map { it.date }.distinct().size,
            totalWallTimeMs = wall,
            averageSpeed = if (wall > 0) bookTime.toFloat() / wall else 1f,
            estimatedFinish = estimate,
        )
    }

    private fun dailyBars(byDate: Map<LocalDate, Long>, today: LocalDate, count: Long): List<PeriodTotal> =
        (count - 1 downTo 0).map { back -> today.minusDays(back).let { PeriodTotal(it, byDate[it] ?: 0L) } }

    private fun monthlyBars(days: List<ListeningDay>, today: LocalDate): List<PeriodTotal> {
        val firstDay = today.minusDays(YEAR - 1)
        val byMonth = days.filter { !it.date.isBefore(firstDay) && !it.date.isAfter(today) }
            .groupBy { YearMonth.from(it.date) }
            .mapValues { (_, entries) -> entries.sumOf { it.wallTimeMs } }
        val thisMonth = YearMonth.from(today)
        return (MONTHS - 1 downTo 0).map { back ->
            val month = thisMonth.minusMonths(back)
            PeriodTotal(month.atDay(1), byMonth[month] ?: 0L)
        }
    }

    private fun streak(byDate: Map<LocalDate, Long>, today: LocalDate): Int {
        fun counts(date: LocalDate) = (byDate[date] ?: 0L) >= STREAK_MINIMUM_MS
        var day = if (counts(today)) today else today.minusDays(1)
        var length = 0
        while (counts(day)) {
            length++
            day = day.minusDays(1)
        }
        return length
    }

    private const val WEEK = 7L
    private const val MONTH = 30L
    private const val YEAR = 365L
    private const val MONTHS = 12L
}
