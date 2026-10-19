package com.listening

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepScheduleTest {
    @Test
    fun windowAcrossMidnight() {
        val schedule = SleepSchedule(startMinuteOfDay = 22 * 60, endMinuteOfDay = 6 * 60)
        assertTrue(schedule.contains(22 * 60))
        assertTrue(schedule.contains(23 * 60 + 59))
        assertTrue(schedule.contains(0))
        assertTrue(schedule.contains(5 * 60 + 59))
        assertFalse(schedule.contains(6 * 60))
        assertFalse(schedule.contains(12 * 60))
    }

    @Test
    fun windowInsideOneDayAndEmptyWindow() {
        val afternoon = SleepSchedule(13 * 60, 15 * 60)
        assertTrue(afternoon.contains(14 * 60))
        assertFalse(afternoon.contains(15 * 60))
        assertFalse(afternoon.contains(12 * 60))
        assertFalse(SleepSchedule(60, 60).contains(60))
    }
}
