package com.mobyle.abbay.data.extras

import com.mobyle.abbay.data.database.dao.ExtrasDao
import com.repository.QueueRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The queue is rewritten as a whole on every change; it holds a handful of books. */
@Singleton
class QueueRepositoryImpl @Inject constructor(private val dao: ExtrasDao) : QueueRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mutex = Mutex()

    override fun observeQueue(): Flow<List<String>> = dao.observeQueue().map { ids -> ids.map(Long::toString) }

    override suspend fun add(bookId: String, playNext: Boolean) = edit(bookId) { queue, id ->
        val without = queue - id
        if (playNext) listOf(id) + without else without + id
    }

    override suspend fun remove(bookId: String) = edit(bookId) { queue, id -> queue - id }

    override suspend fun move(bookId: String, index: Int) = edit(bookId) { queue, id ->
        if (id !in queue) {
            queue
        } else {
            (queue - id).toMutableList().apply { add(index.coerceIn(0, size), id) }
        }
    }

    override suspend fun takeNext(): String? = withContext(ioDispatcher) {
        mutex.withLock {
            val queue = dao.getQueue()
            queue.firstOrNull()?.also { dao.replaceQueue(queue.drop(1)) }?.toString()
        }
    }

    private suspend fun edit(bookId: String, change: (List<Long>, Long) -> List<Long>) = withContext(ioDispatcher) {
        val id = bookId.toLongOrNull() ?: return@withContext
        mutex.withLock {
            val queue = dao.getQueue()
            val changed = change(queue, id)
            if (changed != queue) dao.replaceQueue(changed)
        }
    }
}
