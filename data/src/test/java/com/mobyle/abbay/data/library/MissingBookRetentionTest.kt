package com.mobyle.abbay.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MissingBookRetentionTest {
    private val now = TimeUnit.DAYS.toMillis(1_000)

    @Test
    fun `a book is kept for the retention days`() {
        val cutoff = MissingBookRetention.cutoff(retentionDays = 30, now = now)

        assertEquals(now - TimeUnit.DAYS.toMillis(30), cutoff)
        assertFalse(MissingBookRetention.removesAtOnce(30))
    }

    @Test
    fun `no retention removes a book missing since this very moment`() {
        val cutoff = MissingBookRetention.cutoff(retentionDays = MissingBookRetention.REMOVE_AT_ONCE, now = now)

        assertTrue(now < cutoff)
        assertTrue(MissingBookRetention.removesAtOnce(MissingBookRetention.REMOVE_AT_ONCE))
    }
}
