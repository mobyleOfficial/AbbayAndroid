package com.mobyle.abbay.presentation.readalong

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import com.player.PlayerController
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongSession
import com.readalong.ReadAlongSnapshot
import com.readalong.ReadAlongState
import com.readalong.ReadAlongSync
import com.readalong.ReadAlongTranscriber
import com.repository.ReadAlongRepository
import com.usecase.GetAppLanguage
import com.usecase.ObserveAppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * The player's read-along: one session for the player, kept here rather than in the cover square, which is rebuilt
 * each time the Text pill flips or the window reflows (a session of its own would check the language and load the
 * transcript again each time, flashing *Preparing text…*). The square and the full-screen text both show it, and
 * count themselves in and out ([show], [hide]): the session runs while at least one is on screen, and only then.
 *
 * The shown time is the scrubber's while a finger drags it, else the player's, polled every [POLL_INTERVAL_MS] while
 * the text shows. The text follows it moved by the book's [sync] adjustment, for when the two drift apart.
 *
 * Where the book has an EPUB beside its audio, the lines shown are its text, timed against the transcript
 * ([com.readalong.ReadAlongAligner]); the transcript itself is still what is made and saved, so the EPUB can come and go.
 */
@HiltViewModel
class ReadAlongViewModel @Inject constructor(
    transcriber: ReadAlongTranscriber,
    private val playerController: PlayerController,
    private val repository: ReadAlongRepository,
    observeAppSettings: ObserveAppSettings,
    getAppLanguage: GetAppLanguage,
) : ViewModel() {
    private val session = ReadAlongSession(
        scope = viewModelScope,
        transcriber = transcriber,
        repository = repository,
        preferredLanguages = { deviceLanguages() },
        appLanguage = { getAppLanguage().tag.ifEmpty { Locale.getDefault().toLanguageTag() } },
    )

    /** Cancelling the speech model's download, and asking for it again. */
    val modelDownload = ReadAlongModelDownloadControls(onCancel = session::cancelModelDownload, onRetry = session::retryModelDownload)

    /** The EPUB the shown book's text comes from: the one in its folder, or one picked for it. */
    val bookText = ReadAlongBookTextSource(viewModelScope, repository)

    /** What the text shows: the session's, with the book's own text wherever it matches the audio. */
    val snapshot: StateFlow<ReadAlongSnapshot> = combine(session.snapshot, bookText.aligner, bookText.isLoading) {
            shown,
            bookAligner,
            isLoading
        ->
        val transcript = shown.transcript
        when {
            transcript == null -> shown
            // Until the book's EPUB is known, its text isn't shown as generated: it may be about to become the book's.
            isLoading && shown.state == ReadAlongState.Ready -> shown.copy(state = ReadAlongState.Preparing, transcript = null)
            bookAligner == null -> shown
            else -> shown.copy(transcript = bookAligner.aligned(transcript))
        }
    }.conflate().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, ReadAlongSnapshot())

    /** The listener's correction of the text against the audio, for the book shown. */
    val sync = ReadAlongSyncAdjustment(viewModelScope, repository)

    private val mutableTimeMs = MutableStateFlow(0L)

    /** The time the text follows: the scrubber's under a finger, else the player's. */
    val timeMs: StateFlow<Long> = mutableTimeMs.asStateFlow()

    /** The moment of the text shown: [timeMs] moved by the [sync] adjustment. */
    val textTimeMs: StateFlow<Long> = combine(mutableTimeMs, sync.offsetMs, ReadAlongSync::textTime)
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val scrubTimeMs = MutableStateFlow<Long?>(null)

    val isEnabled: StateFlow<Boolean> = observeAppSettings().map { it.readAlongEnabled }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The square's text or cover, and the explanation before each book's text first shows. */
    val textToggle = ReadAlongTextToggle(
        viewModelScope,
        isIntroSeen = repository::isIntroSeen,
        markIntroSeen = { bookId -> repository.setIntroSeen(bookId, true) },
    )

    private var shownBook: Book? = null
    private var shownCount = 0
    private var polling: Job? = null
    private var filesSignature: List<String> = emptyList()

    init {
        viewModelScope.launch {
            isEnabled.collect { enabled -> if (!enabled) disable() }
        }
    }

    /** A view showing [book]'s text appeared (or the same view now shows another book, or the app came back). */
    fun show(book: Book) {
        if (!isEnabled.value) return
        shownCount += 1
        follow(book)
    }

    /** [book] opened in the player, still on its cover: its EPUB is read now, so the text is the book's from the start. */
    fun prepare(book: Book) {
        if (isEnabled.value) bookText.load(book)
    }

    /** The book shown changed while the text stays (another book loaded, or its files changed). */
    fun follow(book: Book) {
        if (shownCount == 0) return
        val signature = filesOf(book)
        val previous = shownBook
        shownBook = book
        if (previous?.id == book.id && signature != filesSignature) {
            filesSignature = signature
            session.downloadsChanged(book)
            return
        }
        filesSignature = signature
        if (previous?.id != book.id) {
            sync.load(book.id)
            bookText.load(book)
        }
        if (previous?.id != book.id || polling == null) {
            mutableTimeMs.value = timeOf(book)
            session.show(book, mutableTimeMs.value)
            startPolling()
        }
    }

    /** A view showing the text left the screen; the session stops once none shows it. */
    fun hide() {
        shownCount = (shownCount - 1).coerceAtLeast(0)
        if (shownCount > 0) return
        polling?.cancel()
        polling = null
        session.stop()
    }

    /** The scrubber's finger is at [bookPositionMs], or lifted (null). */
    fun scrub(bookPositionMs: Long?) {
        scrubTimeMs.value = bookPositionMs
        if (bookPositionMs != null && shownCount > 0) {
            mutableTimeMs.value = bookPositionMs
            session.timeChanged(bookPositionMs, scrubbing = true)
        }
    }

    fun setLanguage(locale: String) = session.setLanguage(locale)

    /** The listener says [line] is the one being read now. */
    fun alignTo(line: ReadAlongLine) = sync.align(line, timeMs.value)

    private fun startPolling() {
        polling?.cancel()
        polling = viewModelScope.launch {
            while (isActive) {
                val book = shownBook ?: break
                val scrubbed = scrubTimeMs.value
                val time = scrubbed ?: timeOf(book)
                mutableTimeMs.value = time
                session.timeChanged(time, scrubbing = scrubbed != null)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun disable() {
        polling?.cancel()
        polling = null
        shownBook = null
        bookText.load(null)
        textToggle.showCover()
        session.disable()
    }

    /** The player's position when it has this book loaded; the book's saved position otherwise. */
    private fun timeOf(book: Book): Long =
        if (playerController.state.value.bookId == book.id) playerController.bookPositionMs() else book.bookPositionMs

    override fun onCleared() {
        session.stop()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 100L
    }
}

/** The book's files, to notice one finishing downloading (its uri turns local) while the text shows. */
private fun filesOf(book: Book): List<String> = when (book) {
    is MultipleBooks -> book.bookFileList.map { it.uri }
    is BookFile -> listOf(book.uri)
    else -> emptyList()
}

/** The device's languages, most preferred first, whatever language the app itself is set to. */
private fun deviceLanguages(): List<String> {
    val locales = Resources.getSystem().configuration.locales
    return (0 until locales.size()).map { locales[it].toLanguageTag() }
}
