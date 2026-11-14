package com.mobyle.abbay.presentation.audiobookshelf

import com.model.AudiobookshelfPage
import com.model.DataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Whether a long list can grow: more to load, loading it, or the last page failed and can be tried again. */
enum class MoreItems {
    NONE,
    AVAILABLE,
    LOADING,
    FAILED,
}

/** A list read page by page from the server, kept while the screen is open; it loads its first page at once. */
class PagedShelf<T>(private val scope: CoroutineScope, private val loadPage: suspend (page: Int) -> DataResult<AudiobookshelfPage<T>>) {
    data class State<T>(val items: List<T> = emptyList(), val more: MoreItems = MoreItems.LOADING)

    private val mutableState = MutableStateFlow(State<T>())
    val state: StateFlow<State<T>> = mutableState.asStateFlow()

    private var nextPage = 0
    private var job: Job? = null

    init {
        loadMore()
    }

    /** Loads the next page, unless one is loading or there is none; after a failure it tries that page again. */
    fun loadMore() {
        val more = mutableState.value.more
        if (job?.isActive == true || more == MoreItems.NONE) return
        mutableState.update { it.copy(more = MoreItems.LOADING) }
        job = scope.launch {
            when (val result = loadPage(nextPage)) {
                is DataResult.Failure -> mutableState.update { it.copy(more = MoreItems.FAILED) }
                is DataResult.Success -> {
                    nextPage++
                    mutableState.update { current ->
                        State(
                            items = current.items + result.value.items,
                            more = if (result.value.hasMore) MoreItems.AVAILABLE else MoreItems.NONE,
                        )
                    }
                }
            }
        }
    }
}
