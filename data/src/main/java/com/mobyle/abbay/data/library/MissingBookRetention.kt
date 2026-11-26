package com.mobyle.abbay.data.library

import java.util.concurrent.TimeUnit

/** How long a book whose files disappeared stays in the library before a scan removes it. */
internal object MissingBookRetention {
    /** The retention setting that removes a missing book at the first scan that misses it. */
    const val REMOVE_AT_ONCE = 0

    /** Books missing since before this time are removed. */
    fun cutoff(retentionDays: Int, now: Long): Long =
        if (retentionDays <= REMOVE_AT_ONCE) Long.MAX_VALUE else now - TimeUnit.DAYS.toMillis(retentionDays.toLong())

    fun removesAtOnce(retentionDays: Int): Boolean = retentionDays <= REMOVE_AT_ONCE
}
