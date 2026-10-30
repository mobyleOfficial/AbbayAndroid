package com.listening

data class SleepSchedule(val startMinuteOfDay: Int, val endMinuteOfDay: Int) {
    fun contains(minuteOfDay: Int): Boolean = when {
        startMinuteOfDay == endMinuteOfDay -> false
        startMinuteOfDay < endMinuteOfDay -> minuteOfDay in startMinuteOfDay until endMinuteOfDay
        else -> minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
