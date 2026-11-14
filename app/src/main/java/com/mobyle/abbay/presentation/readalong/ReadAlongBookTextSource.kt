package com.mobyle.abbay.presentation.readalong

import com.model.Book
import com.readalong.ReadAlongAligner
import com.readalong.ReadAlongEpub
import com.repository.ReadAlongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The EPUB the shown book's text comes from, ready to time against its transcript. One in the audiobook's folder is
 * used by default; without one the listener can pick one from anywhere ([choose]) and drop it again ([remove]).
 */
class ReadAlongBookTextSource(private val scope: CoroutineScope, private val repository: ReadAlongRepository) {
    private val mutableEpub = MutableStateFlow<ReadAlongEpub?>(null)
    val epub: StateFlow<ReadAlongEpub?> = mutableEpub.asStateFlow()

    private val mutableAligner = MutableStateFlow<ReadAlongAligner?>(null)

    /** The EPUB's text, indexed; null without one (or while it is read), when the transcript shows as it is. */
    val aligner: StateFlow<ReadAlongAligner?> = mutableAligner.asStateFlow()

    private val mutableIsRejected = MutableStateFlow(false)

    /** The file just picked couldn't be read as an EPUB; [rejectionShown] once the listener was told. */
    val isRejected: StateFlow<Boolean> = mutableIsRejected.asStateFlow()

    private val mutableIsLoading = MutableStateFlow(false)

    /** The book's EPUB is still being looked for and read: its text isn't known to be generated yet. */
    val isLoading: StateFlow<Boolean> = mutableIsLoading.asStateFlow()

    private var book: Book? = null
    private var loading: Job? = null

    /**
     * [shown] is the player's book: its EPUB, if any, is read and indexed off the main thread, ahead of the text being
     * asked for. The same book again keeps what is loaded or loading, unless [reload].
     */
    fun load(shown: Book?, reload: Boolean = false) {
        if (!reload && shown != null && shown.id == book?.id) return
        book = shown
        loading?.cancel()
        mutableEpub.value = null
        mutableAligner.value = null
        mutableIsLoading.value = shown != null
        if (shown == null) return
        loading = scope.launch {
            val epub = repository.epub(shown)
            val aligner = epub?.let {
                repository.bookText(shown)
            }?.let { text -> withContext(Dispatchers.Default) { ReadAlongAligner(text) } }
            if (book?.id == shown.id) {
                mutableEpub.value = epub
                mutableAligner.value = aligner
                mutableIsLoading.value = false
            }
        }
    }

    /** The listener picked [uri] for the shown book. */
    fun choose(uri: String) = change(uri)

    /** The chosen EPUB is dropped: the text is generated again. */
    fun remove() = change(null)

    fun rejectionShown() {
        mutableIsRejected.value = false
    }

    private fun change(uri: String?) {
        val shown = book ?: return
        scope.launch {
            val isAccepted = repository.chooseEpub(shown.id, uri)
            if (!isAccepted) mutableIsRejected.value = true
            if (book?.id == shown.id) load(shown, reload = true)
        }
    }
}
