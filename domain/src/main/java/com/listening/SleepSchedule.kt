package com.listening

/** Daily window in which playback starts the sleep timer by itself. */
data class SleepSchedule(val startMinuteOfDay: Int, val endMinuteOfDay: Int) {
    /** True when [minuteOfDay] is inside the window; a window like 22:00–06:00 crosses midnight. */
    fun contains(minuteOfDay: Int): Boolean = when {
        startMinuteOfDay == endMinuteOfDay -> false
        startMinuteOfDay < endMinuteOfDay -> minuteOfDay in startMinuteOfDay until endMinuteOfDay
        else -> minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
