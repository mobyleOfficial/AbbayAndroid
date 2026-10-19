package com.mobyle.abbay.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.Book
import com.usecase.MoveInQueue
import com.usecase.ObserveQueue
import com.usecase.RemoveFromQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The queue of books to play next. */
@HiltViewModel
class QueueViewModel @Inject constructor(
    observeQueue: ObserveQueue,
    private val removeFromQueue: RemoveFromQueue,
    private val moveInQueue: MoveInQueue,
) : ViewModel() {
    /** Null until the database answers. */
    val queue: StateFlow<List<Book>?> = observeQueue().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun remove(book: Book) = viewModelScope.launch { removeFromQueue(book.id) }

    fun move(book: Book, index: Int) = viewModelScope.launch { moveInQueue(book.id, index) }
}
