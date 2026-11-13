package com.mobyle.abbay.presentation.readalong

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether the player's square shows the text or the cover. It is the player's moment, not a setting: a book always
 * opens on its cover ([showCover]). The first time each book's text is asked for, [isIntroShown] explains where it
 * comes from (the book's EPUB, else text generated from the audio), and the text shows once that is read.
 */
class ReadAlongTextToggle(
    private val scope: CoroutineScope,
    private val isIntroSeen: suspend (bookId: String) -> Boolean,
    private val markIntroSeen: suspend (bookId: String) -> Unit,
) {
    private val mutableShowsText = MutableStateFlow(false)
    val showsText: StateFlow<Boolean> = mutableShowsText.asStateFlow()

    private val mutableIsIntroShown = MutableStateFlow(false)
    val isIntroShown: StateFlow<Boolean> = mutableIsIntroShown.asStateFlow()

    /** The book whose text was asked for, while its explanation shows. */
    private var introBookId: String? = null
    private var checking: Job? = null

    /** The Text/Cover pill on [bookId]'s player: back to the cover, or to the text once its source has been explained. */
    fun toggle(bookId: String) {
        if (mutableShowsText.value) {
            mutableShowsText.value = false
            return
        }
        checking?.cancel()
        checking = scope.launch {
            if (isIntroSeen(bookId)) {
                mutableShowsText.value = true
            } else {
                introBookId = bookId
                mutableIsIntroShown.value = true
            }
        }
    }

    /** The explanation was read: it won't show again for this book, and the text shows now. */
    fun confirmIntro() {
        val bookId = introBookId
        mutableIsIntroShown.value = false
        mutableShowsText.value = true
        introBookId = null
        if (bookId != null) scope.launch { markIntroSeen(bookId) }
    }

    /** Dismissed without asking for the text: the cover stays, and the explanation shows again next time. */
    fun dismissIntro() {
        mutableIsIntroShown.value = false
        introBookId = null
    }

    /** Another book opened, the player closed, or read-along was turned off: the cover shows. */
    fun showCover() {
        checking?.cancel()
        mutableShowsText.value = false
        mutableIsIntroShown.value = false
        introBookId = null
    }
}
