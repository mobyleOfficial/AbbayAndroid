package com.mobyle.abbay.presentation.statistics

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R

/** "3 h 20 min" or "45 min". */
@Composable
fun listeningTime(durationMs: Long): String {
    val totalMinutes = (durationMs / MS_PER_MINUTE).coerceAtLeast(0L)
    val hours = (totalMinutes / MINUTES_PER_HOUR).toInt()
    val minutes = (totalMinutes % MINUTES_PER_HOUR).toInt()
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
