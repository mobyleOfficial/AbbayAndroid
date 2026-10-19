package com.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StatisticsAggregatorTest {
    private val today = LocalDate.of(2026, 10, 17)
    private val minute = 60_000L

    @Test
    fun totalsBarsTopBooksAndSavings() {
        val days = listOf(
            day("a", 0, wall = 30 * minute, book = 45 * minute, speed = 15 * minute, silence = 2 * minute),
            day("b", 0, wall = 10 * minute, book = 10 * minute),
            day("a", 3, wall = 20 * minute, book = 30 * minute, speed = 10 * minute),
            day("c", 40, wall = 60 * minute, book = 60 * minute),
            day("a", 400, wall = 99 * minute, book = 99 * minute),
        )
        val stats = StatisticsAggregator.global(days, today)

        assertEquals(219 * minute, stats.totalWallTimeMs)
        assertEquals(7, stats.last7Days.size)
        assertEquals(today, stats.last7Days.last().start)
        assertEquals(40 * minute, stats.last7Days.last().wallTimeMs)
        assertEquals(20 * minute, stats.last7Days[3].wallTimeMs)
        assertEquals(30, stats.last30Days.size)
        assertEquals(60 * minute, stats.last30Days.sumOf { it.wallTimeMs })
        // 12 monthly bars; the 400-day-old day is outside the last 365 days.
        assertEquals(12, stats.last12Months.size)
        assertEquals(120 * minute, stats.last12Months.sumOf { it.wallTimeMs })
        assertEquals(LocalDate.of(2026, 10, 1), stats.last12Months.last().start)
        assertEquals(listOf("a", "c", "b"), stats.topBooks.map { it.bookId })
        assertEquals(25 * minute, stats.savedBySpeedMs)
        assertEquals(2 * minute, stats.savedBySilenceMs)
    }

    @Test
    fun streakCountsDaysWithFiveMinutesUpToTodayOrYesterday() {
        val days = listOf(
            day("a", 1, wall = 6 * minute),
            day("a", 2, wall = 5 * minute),
            day("b", 3, wall = 3 * minute),
            day(
                "b",
                3,
                wall =
                3 * minute
            )
        )
        assertEquals(3, StatisticsAggregator.global(days, today).streakDays)
        assertEquals(4, StatisticsAggregator.global(days + day("a", 0, wall = 5 * minute), today).streakDays)
        assertEquals(0, StatisticsAggregator.global(listOf(day("a", 2, wall = 60 * minute)), today).streakDays)
    }

    @Test
    fun bookSessionsSpeedAndFinishDate() {
        val days = listOf(
            day("a", 0, wall = 40 * minute, book = 60 * minute),
            day("a", 13, wall = 40 * minute, book = 80 * minute),
            day("a", 20, wall = 100 * minute, book = 100 * minute),
        )
        val stats = StatisticsAggregator.book(days, remainingBookTimeMs = 140 * minute, today = today)
        assertEquals(3, stats.sessions)
        assertEquals(240f / 180f, stats.averageSpeed, 0.001f)
        // The last 14 days heard 140 min: 10 min a day, so 140 min take 14 days.
        assertEquals(today.plusDays(14), stats.estimatedFinish)
        assertNull(StatisticsAggregator.book(days, remainingBookTimeMs = 0, today = today).estimatedFinish)
        assertEquals(1f, StatisticsAggregator.book(emptyList(), 10, today).averageSpeed)
    }

    private fun day(bookId: String, daysAgo: Long, wall: Long, book: Long = wall, speed: Long = 0, silence: Long = 0) =
        ListeningDay(
            bookId,
            today.minusDays(daysAgo),
            bookTimeMs = book,
            wallTimeMs = wall,
            savedBySpeedMs = speed,
            savedBySilenceMs = silence
        )
}
