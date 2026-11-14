package com.mobyle.abbay.presentation.playback.policy

import com.mobyle.abbay.presentation.playback.command.BookNavigator
import com.mobyle.abbay.presentation.playback.command.TrackPosition
import com.player.PlaybackOptions
import com.player.RepeatMode

@Suppress("ReturnCount")
class PlaybackRules(private val trackDurationsMs: List<Long>, private val chapterStartsMs: List<Long>) {
    private val navigator = BookNavigator(trackDurationsMs, chapterStartsMs)

    fun decide(options: PlaybackOptions, previous: TrackPosition, current: TrackPosition): TrackPosition? {
        val before = navigator.bookPositionOf(previous)
        val now = navigator.bookPositionOf(current)
        if (now < before) return null

        if (options.hasLoop) {
            val loopA = requireNotNull(options.loopAMs)
            val loopB = requireNotNull(options.loopBMs)
            if (before in loopA until loopB && now >= loopB) return navigator.positionAt(loopA)
        }
        if (options.repeatMode == RepeatMode.CHAPTER) {
            val chapter = navigator.chapterIndexAt(before)
            if (navigator.chapterIndexAt(now) != chapter || now >= navigator.bookDurationMs) {
                return navigator.positionAt(chapterStart(chapter))
            }
        }
        return outroTarget(options, previous, current)
    }

    fun introTarget(options: PlaybackOptions, current: TrackPosition): TrackPosition? {
        val introMs = options.skipIntroSeconds * MILLISECONDS_PER_SECOND
        if (introMs <= 0 || current.positionMs > START_TOLERANCE_MS) return null
        val duration = trackDurationsMs.getOrNull(current.trackIndex) ?: return null
        return if (introMs < duration) TrackPosition(current.trackIndex, introMs) else null
    }

    private fun outroTarget(options: PlaybackOptions, previous: TrackPosition, current: TrackPosition): TrackPosition? {
        val outroMs = options.skipOutroSeconds * MILLISECONDS_PER_SECOND
        if (outroMs <= 0 || previous.trackIndex != current.trackIndex) return null
        val duration = trackDurationsMs.getOrNull(current.trackIndex)?.takeIf { it > outroMs } ?: return null
        val outroStart = duration - outroMs
        if (previous.positionMs >= outroStart || current.positionMs < outroStart) return null
        return when {
            options.repeatMode == RepeatMode.TRACK -> TrackPosition(current.trackIndex, 0L)
            current.trackIndex < trackDurationsMs.lastIndex -> TrackPosition(current.trackIndex + 1, 0L)
            options.repeatMode == RepeatMode.BOOK -> TrackPosition(0, 0L)
            else -> TrackPosition(current.trackIndex, duration)
        }
    }

    private fun chapterStart(index: Int): Long = navigator.chapterStartAt(index)

    companion object {
        const val START_TOLERANCE_MS = 500L
        private const val MILLISECONDS_PER_SECOND = 1_000L
    }
}
