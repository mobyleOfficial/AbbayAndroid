package com.mobyle.abbay.presentation.subtitles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.PlayerController
import com.subtitles.SubtitleTimeline
import com.subtitles.TrackSubtitles
import com.usecase.GetTrackSubtitles
import com.usecase.ObserveBooks
import com.usecase.SetSubtitleCharset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TextPaneViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerController: PlayerController,
    private val getTrackSubtitles: GetTrackSubtitles,
    private val setSubtitleCharset: SetSubtitleCharset,
    private val observeBooks: ObserveBooks,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[BOOK_ID_ARGUMENT])

    sealed interface State {
        data object Loading : State

        data object NoText : State

        data class Text(val subtitles: TrackSubtitles, val currentIndex: Int) : State
    }

    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var loadedTrack = -1
    private var hasRequestedLoad = false

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun seekTo(startMs: Long) {
        playerController.seekTo(startMs)
        viewModelScope.launch { refresh() }
    }

    fun chooseCharset(charset: String) = viewModelScope.launch {
        setSubtitleCharset(bookId, charset)
        loadedTrack = -1
        refresh()
    }

    private suspend fun refresh() {
        val playerState = playerController.state.value
        if (playerState.bookId != bookId) {
            if (playerState.isConnected && !hasRequestedLoad) {
                hasRequestedLoad = true
                observeBooks().first().firstOrNull { it.id == bookId }?.let { playerController.openBook(it, playWhenReady = false) }
            }
            return
        }
        val track = playerState.currentTrackIndex
        if (track != loadedTrack) {
            loadedTrack = track
            val subtitles = getTrackSubtitles(bookId, track)
            mutableState.value = subtitles?.let { State.Text(it, -1) } ?: State.NoText
        }
        val current = mutableState.value as? State.Text ?: return
        val index = SubtitleTimeline.indexAt(current.subtitles.lines, playerController.currentPositionMs())
        if (index != current.currentIndex) mutableState.value = current.copy(currentIndex = index)
    }

    companion object {
        const val BOOK_ID_ARGUMENT = "bookId"
        private const val POLL_INTERVAL_MS = 250L

        val CHARSETS = listOf("windows-1252", "ISO-8859-15", "windows-1250", "windows-1251", "KOI8-R", "ISO-8859-7") +
            listOf("Shift_JIS", "GBK", "Big5", "EUC-KR")
    }
}
