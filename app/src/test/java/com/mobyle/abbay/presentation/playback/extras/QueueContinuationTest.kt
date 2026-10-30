package com.mobyle.abbay.presentation.playback.extras

import com.model.Book
import com.model.BookState
import com.model.BookType
import com.model.Chapter
import com.player.UpNext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueContinuationTest {
    private val queue = ArrayDeque(listOf<Book>(TestBook("2", "Second")))
    private val published = mutableListOf<UpNext?>()
    private val started = mutableListOf<String>()

    @Test
    fun countsDownThreeSecondsThenStartsTheNextBook() = runTest {
        val continuation = continuation(this)
        continuation.onBookFinished()
        runCurrent()
        assertEquals(UpNext("2", "Second", 3), continuation.upNext)

        advanceTimeBy(2_999)
        runCurrent()
        assertTrue(started.isEmpty())
        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf("2"), started)
        assertEquals(listOf(3, 2, 1, null), published.map { it?.secondsLeft })
    }

    @Test
    fun cancelPutsTheBookBackAndStartsNothing() = runTest {
        val continuation = continuation(this)
        continuation.onBookFinished()
        advanceTimeBy(1_500)
        continuation.cancel()
        advanceTimeBy(5_000)
        runCurrent()

        assertTrue(started.isEmpty())
        assertEquals(listOf("2"), queue.map { it.id })
        assertEquals(null, continuation.upNext)
    }

    @Test
    fun turnedOffOrEmptyQueueDoesNothing() = runTest {
        continuation(this, enabled = false).onBookFinished()
        queue.clear()
        continuation(this).onBookFinished()
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(started.isEmpty())
        assertTrue(published.isEmpty())
    }

    private fun continuation(scope: kotlinx.coroutines.CoroutineScope, enabled: Boolean = true) = QueueContinuation(
        scope = scope,
        isEnabled = { enabled },
        takeNext = { queue.removeFirstOrNull() },
        putBack = { id -> queue.addFirst(TestBook(id, "Second")) },
        onChanged = { published += it },
        start = { started += it.id },
    )

    private data class TestBook(override val id: String, override val name: String) : Book {
        override val thumbnail: String? = null
        override val progress = 0L
        override val duration = 1_000L
        override val speed = 1f
        override val hasError = false
        override val type = BookType.FILE
        override val author: String? = null
        override val state = BookState.NEW
        override val addedAt = 0L
        override val lastPlayedAt: Long? = null
        override val bookPositionMs = 0L
        override val chapters: List<Chapter> = emptyList()
    }
}
