package com.mobyle.abbay.presentation.playback.policy

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.mobyle.abbay.presentation.playback.command.TrackPosition
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.mobyle.abbay.presentation.playback.media.Media3PlaylistPlayer
import com.player.PlaybackOptions
import com.player.RepeatMode
import com.usecase.ObservePlaybackOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRulesCoordinator(
    private val player: Player,
    private val scope: CoroutineScope,
    observePlaybackOptions: ObservePlaybackOptions,
    private val withoutMoves: (() -> Unit) -> Unit,
) : Player.Listener {
    private val playlist = Media3PlaylistPlayer(player)
    private val bookId = MutableStateFlow<String?>(null)
    private var options = PlaybackOptions()
    private var ticker: Job? = null
    private var lastSample: TrackPosition? = null

    init {
        bookId.flatMapLatest { id -> id?.let(observePlaybackOptions::invoke) ?: emptyFlow() }
            .onEach { newOptions ->
                options = newOptions
                player.repeatMode = when (newOptions.repeatMode) {
                    RepeatMode.TRACK -> Player.REPEAT_MODE_ONE
                    RepeatMode.BOOK -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                updateTicker()
            }
            .launchIn(scope)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        bookId.value = BookMediaItems.bookIdOf(mediaItem)
        lastSample = null
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
        rules().introTarget(options, playlist.currentPosition)?.let(::jump)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = updateTicker()

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) lastSample = null
    }

    private fun updateTicker() {
        val needsTicks = player.isPlaying &&
            (options.repeatMode == RepeatMode.CHAPTER || options.hasLoop || options.skipOutroSeconds > 0)
        if (!needsTicks) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                val current = playlist.currentPosition
                val previous = lastSample
                lastSample = current
                if (previous != null) rules().decide(options, previous, current)?.let(::jump)
                delay(TICK_MS)
            }
        }
    }

    private fun jump(target: TrackPosition) {
        lastSample = null
        withoutMoves { playlist.seekTo(target) }
    }

    private fun rules() = PlaybackRules(playlist.trackDurationsMs, playlist.chapterStartsMs)

    private companion object {
        const val TICK_MS = 100L
    }
}
