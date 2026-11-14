package com.mobyle.abbay.fakes

import com.repository.QueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeQueueRepository : QueueRepository {
    val queue = MutableStateFlow(emptyList<String>())

    override fun observeQueue(): Flow<List<String>> = queue

    override suspend fun add(bookId: String, playNext: Boolean) {
        val without = queue.value - bookId
        queue.value = if (playNext) listOf(bookId) + without else without + bookId
    }

    override suspend fun remove(bookId: String) {
        queue.value -= bookId
    }

    override suspend fun move(bookId: String, index: Int) {
        val without = (queue.value - bookId).toMutableList()
        without.add(index.coerceIn(0, without.size), bookId)
        queue.value = without
    }

    override suspend fun takeNext(): String? = queue.value.firstOrNull()?.also { queue.value = queue.value.drop(1) }
}
