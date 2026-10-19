package com.mobyle.abbay.presentation.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.PlaybackOptions
import com.player.PlayerController
import com.player.RepeatMode
import com.usecase.ObservePlaybackOptions
import com.usecase.UpdatePlaybackOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Repeat, A–B loop and skip intro/outro of the book in the player. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaybackOptionsViewModel @Inject constructor(
    observePlaybackOptions: ObservePlaybackOptions,
    private val updatePlaybackOptions: UpdatePlaybackOptions,
    private val playerController: PlayerController,
) : ViewModel() {
    private val bookId = MutableStateFlow<String?>(null)

    val options: StateFlow<PlaybackOptions> = bookId
        .flatMapLatest { id -> id?.let(observePlaybackOptions::invoke) ?: flowOf(PlaybackOptions()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackOptions())

    fun load(bookId: String?) {
        this.bookId.value = bookId
    }

    fun change(options: PlaybackOptions) {
        val id = bookId.value ?: return
        viewModelScope.launch { updatePlaybackOptions(id, options) }
    }

    /**
     * Sets loop point A (or B) at the player's position and turns the A–B loop on. Playback is at B when B is set, so it
     * goes back to A at once; from then on the service loops whenever playback reaches B.
     */
    fun setLoopPoint(isStart: Boolean) {
        val position = playerController.bookPositionMs()
        val current = options.value
        val updated = if (isStart) {
            current.copy(repeatMode = RepeatMode.AB, loopAMs = position)
        } else {
            current.copy(repeatMode = RepeatMode.AB, loopBMs = position)
        }
        change(updated)
        if (!isStart && updated.hasLoop) playerController.seekToBookPosition(requireNotNull(updated.loopAMs))
    }
}
