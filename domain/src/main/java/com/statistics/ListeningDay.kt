package com.statistics

import java.time.LocalDate

data class ListeningDay(
    val bookId: String,
    val date: LocalDate,
    val bookTimeMs: Long,
    val wallTimeMs: Long,
    val savedBySpeedMs: Long = 0L,
    val savedBySilenceMs: Long = 0L,
)
