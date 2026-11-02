package com.mobyle.abbay.presentation.readalong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.Book
import com.player.PlayerController
import com.readalong.ReadAlongLanguage
import com.settings.AppLanguage
import com.settings.AppSettings
import com.usecase.ClearReadAlong
import com.usecase.GetAppLanguage
import com.usecase.ObserveAppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** What the player's text pane draws, and whether it is up at all. */
data class ReadAlongUi(
    val snapshot: ReadAlongSnapshot = ReadAlongSnapshot(),
    val isEnabled: Boolean = false,
    val isShowingText: Boolean = false,
    val isFullScreen: Boolean = false,
) {
    /** The pill and the pane only exist while the feature is on. */
    val isAvailable: Boolean get() = isEnabled
}

/**
 * The player's read-along: holds the one session of the screen, follows playback while the text is on screen, and
 * remembers whether the text or the cover is showing.
 *
 * The session runs only while something shows the text. Both the square over the cover and the full screen
 * count ([acquire] and [release]), so a reflow that builds the new one before dropping the old one doesn't stop
 * the text it is showing.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class ReadAlongViewModel @Inject constructor(
    private val playerController: PlayerController,
    dependencies: ReadAlongDependencies,
    observeAppSettings: ObserveAppSettings,
    getAppLanguage: GetAppLanguage,
    private val clearReadAlong: ClearReadAlong,
) : ViewModel() {
    private val settings: StateFlow<AppSettings> = observeAppSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val session = ReadAlongSession(dependencies, viewModelScope) {
        ReadAlongLanguage.ofCode(
            when (val language = getAppLanguage()) {
                AppLanguage.SYSTEM -> Locale.getDefault().language
                else -> language.tag
            },
        )
    }

    private val mutableUi = MutableStateFlow(ReadAlongUi())
    val ui: StateFlow<ReadAlongUi> = mutableUi.asStateFlow()

    private var book: Book? = null
    private var onScreen = 0
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            settings.collect { appSettings ->
                val isEnabled = appSettings.readAlongEnabled
                if (!isEnabled && mutableUi.value.isShowingText) stopShowing()
                if (!isEnabled && mutableUi.value.isEnabled) {
                    // Read-along was turned off: nothing it made stays behind.
                    session.forget()
                    clearReadAlong()
                }
                update { it.copy(isEnabled = isEnabled) }
            }
        }
    }

    /** The text or the cover, as the pill flips it. */
    fun toggleText() {
        if (!mutableUi.value.isAvailable) return
        if (mutableUi.value.isShowingText) stopShowing() else startShowing()
    }

    fun showCover() {
        if (mutableUi.value.isShowingText) stopShowing()
    }

    fun setFullScreen(fullScreen: Boolean) = update { it.copy(isFullScreen = fullScreen && it.isShowingText) }

    fun selectLanguage(language: ReadAlongLanguage) = session.setLanguage(language)

    /** A tap on a line plays from its start, which the transcript keeps in book time. */
    fun seekTo(startMs: Long) = playerController.seekToBookPosition(startMs)

    /** The text is on screen: the session runs, from where the player is now. */
    fun acquire(book: Book) {
        this.book = book
        onScreen++
        session.setDuration(book.duration)
        session.show(book.id, playerController.bookPositionMs())
        startTicker()
        publish(force = true)
    }

    /** The text left the screen. Nothing transcribes once nothing shows it. */
    fun release() {
        onScreen = (onScreen - 1).coerceAtLeast(0)
        if (onScreen > 0) return
        ticker?.cancel()
        ticker = null
        session.stop()
        publish(force = true)
    }

    /** The player closed, or another book took over. */
    fun playerClosed() {
        stopShowing()
    }

    /** The pane composes next, and it is what starts the session (it knows the book). */
    private fun startShowing() = update { it.copy(isShowingText = true) }

    private fun stopShowing() {
        update { it.copy(isShowingText = false, isFullScreen = false) }
        onScreen = 0
        ticker?.cancel()
        ticker = null
        session.stop()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (isActive) {
                book?.let {
                    session.timeChanged(playerController.bookPositionMs())
                    publish()
                }
                delay(TICK_MS)
            }
        }
    }

    private fun update(transform: (ReadAlongUi) -> ReadAlongUi) {
        mutableUi.value = transform(mutableUi.value)
    }

    /**
     * Publishes the session's snapshot when what the pane draws changed: the state, the language, the line or the
     * word. Polling every tenth of a second must not re-render the rest of the player, and a whole book's lines
     * must not be compared on every tick.
     */
    private fun publish(force: Boolean = false) {
        val snapshot = session.snapshot()
        val current = mutableUi.value.snapshot
        val changed = force ||
            current.state != snapshot.state ||
            current.language != snapshot.language ||
            current.display != snapshot.display ||
            current.lines.size != snapshot.lines.size
        if (!changed) return
        update { it.copy(snapshot = snapshot.copy(lines = snapshot.lines.toList())) }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}
