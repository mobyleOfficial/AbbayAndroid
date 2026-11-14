package com.mobyle.abbay.presentation.playback.extras

import com.model.Book
import com.player.UpNext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QueueContinuation(
    private val scope: CoroutineScope,
    private val isEnabled: () -> Boolean,
    private val takeNext: suspend () -> Book?,
    private val putBack: suspend (bookId: String) -> Unit,
    private val onChanged: (UpNext?) -> Unit,
    private val start: (Book) -> Unit,
) {
    var upNext: UpNext? = null
        private set

    private var pending: Book? = null
    private var job: Job? = null

    fun onBookFinished() {
        if (!isEnabled() || job?.isActive == true) return
        job = scope.launch {
            val book = takeNext() ?: return@launch
            pending = book
            for (secondsLeft in COUNTDOWN_SECONDS downTo 1) {
                publish(UpNext(book.id, book.name, secondsLeft))
                delay(SECOND_MS)
            }
            pending = null
            publish(null)
            start(book)
        }
    }

    fun cancel() {
        val book = pending ?: return
        pending = null
        job?.cancel()
        publish(null)
        scope.launch { putBack(book.id) }
    }

    private fun publish(value: UpNext?) {
        upNext = value
        onChanged(value)
    }

    companion object {
        const val COUNTDOWN_SECONDS = 3
        private const val SECOND_MS = 1_000L
    }
}
