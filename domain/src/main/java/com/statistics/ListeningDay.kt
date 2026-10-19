package com.statistics

import java.time.LocalDate

/**
 * Listening of one book on one local day: book time (what was heard) and
 * wall-clock time (how long it took; less at higher speeds), and the time saved by speed and by silence trimming.
 */
data class ListeningDay(
    val bookId: String,
    val date: LocalDate,
    val bookTimeMs: Long,
    val wallTimeMs: Long,
    val savedBySpeedMs: Long = 0L,
    val savedBySilenceMs: Long = 0L,
)
