package com.mobyle.abbay.presentation.readalong

import com.readalong.ReadAlongLine
import com.readalong.ReadAlongSync
import com.repository.ReadAlongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The shown book's sync adjustment ([ReadAlongSync]): read when a book is shown, saved for that book on each change,
 * so it is there the next time the book plays.
 */
class ReadAlongSyncAdjustment(private val scope: CoroutineScope, private val repository: ReadAlongRepository) {
    private val mutableOffsetMs = MutableStateFlow(0L)
    val offsetMs: StateFlow<Long> = mutableOffsetMs.asStateFlow()

    private var bookId: String? = null
    private var loading: Job? = null

    /** [id]'s adjustment, once read; none meanwhile, rather than the previous book's. */
    fun load(id: String) {
        bookId = id
        loading?.cancel()
        mutableOffsetMs.value = 0
        loading = scope.launch { mutableOffsetMs.value = repository.syncOffset(id) }
    }

    /** Some taps of the earlier (positive) or later (negative) buttons. */
    fun nudge(steps: Int) = change(ReadAlongSync.nudged(offsetMs.value, steps))

    fun reset() = change(0)

    /** The listener says [line] is the one being read, the audio being at [audioMs]. */
    fun align(line: ReadAlongLine, audioMs: Long) = change(ReadAlongSync.aligning(line, audioMs))

    /** Where a tap on [line] seeks: where the audio says it, which is its start moved back by the adjustment. */
    fun audioTimeOf(line: ReadAlongLine): Long = ReadAlongSync.audioTime(line.startMs, offsetMs.value)

    private fun change(offset: Long) {
        val id = bookId ?: return
        loading?.cancel()
        mutableOffsetMs.value = offset
        scope.launch { repository.setSyncOffset(id, offset) }
    }
}
