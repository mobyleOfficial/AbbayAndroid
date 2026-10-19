package com.mobyle.abbay.presentation.playback.effects

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.effects.AudioEffects
import com.mobyle.abbay.presentation.playback.command.BookNavigator
import com.mobyle.abbay.presentation.playback.command.TrackPosition
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.usecase.AddSilenceSaved
import com.usecase.ObserveAudioEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Feeds the audio processors with the effects of the book being played: the
 * book's settings from the database, the protected edges of the current track, and the pitch offset as playback
 * parameters. Time saved by silence trimming is added to the book's total when the position is saved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EffectsCoordinator(
    private val player: Player,
    private val state: EffectsState,
    scope: CoroutineScope,
    private val persistenceScope: CoroutineScope,
    observeAudioEffects: ObserveAudioEffects,
    private val addSilenceSaved: AddSilenceSaved,
    /** Also told about saved silence: the statistics count it per day. */
    private val onSilenceSaved: (bookId: String, savedMs: Long) -> Unit = { _, _ -> },
) : Player.Listener {
    private val bookId = MutableStateFlow<String?>(null)

    init {
        bookId.flatMapLatest { id -> id?.let(observeAudioEffects::invoke) ?: emptyFlow() }
            .onEach(::apply)
            .launchIn(scope)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val id = BookMediaItems.bookIdOf(mediaItem)
        if (id != bookId.value) flushSavedSilence()
        bookId.value = id
        state.trackBoundaries = boundariesOf(player.currentMediaItemIndex)
    }

    /** Adds the silence saved since the last call to the current book (called with every position save). */
    fun flushSavedSilence() {
        val id = bookId.value ?: return
        val savedMs = state.takeSavedSilenceMs()
        if (savedMs <= 0) return
        onSilenceSaved(id, savedMs)
        persistenceScope.launch { addSilenceSaved(id, savedMs) }
    }

    private fun apply(effects: AudioEffects) {
        state.current = effects
        val pitch = AudioEffects.pitchFactor(effects.pitchSemitones)
        if (player.playbackParameters.pitch != pitch) player.playbackParameters = PlaybackParameters(player.playbackParameters.speed, pitch)
    }

    /** Track duration and the chapter starts that fall inside the track, in track time. */
    private fun boundariesOf(trackIndex: Int): EffectsState.TrackBoundaries {
        if (trackIndex < 0 || trackIndex >= player.mediaItemCount) return EffectsState.TrackBoundaries.NONE
        val durations = (0 until player.mediaItemCount).map { BookMediaItems.trackDurationOf(player.getMediaItemAt(it)) }
        val trackStart = BookNavigator(durations).bookPositionOf(TrackPosition(trackIndex, 0L))
        val duration = durations[trackIndex]
        val edges = BookMediaItems.chapterStartsOf(player.getMediaItemAt(trackIndex))
            .map { it - trackStart }
            .filter { it in 1 until duration }
            .map { it * MICROSECONDS_PER_MILLISECOND }
        return EffectsState.TrackBoundaries(duration * MICROSECONDS_PER_MILLISECOND, edges)
    }

    private companion object {
        const val MICROSECONDS_PER_MILLISECOND = 1_000L
    }
}
