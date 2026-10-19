package com.mobyle.abbay.presentation.playback.command

/**
 * Executes the player commands on a [PlaylistPlayer]. Chapter navigation uses the
 * tracks as chapters until chapter parsing exists.
 */
@Suppress("TooManyFunctions") // One function per player command.
class PlaybackCommandHandler(
    private val player: PlaylistPlayer,
    private val smallBackMs: () -> Long,
    private val smallForwardMs: () -> Long,
) {
    private val navigator get() = BookNavigator(player.trackDurationsMs, player.chapterStartsMs)

    fun playPause() = if (player.isPlaying) player.pause() else player.play()

    fun seekBy(deltaMs: Long) {
        if (hasTracks()) player.seekTo(navigator.seekBy(player.currentPosition, deltaMs))
    }

    fun seekToBookPosition(bookPositionMs: Long) {
        if (hasTracks()) player.seekTo(navigator.positionAt(bookPositionMs))
    }

    fun skipBack() = seekBy(-smallBackMs())

    fun skipForward() = seekBy(smallForwardMs())

    fun nextTrack() {
        navigator.nextTrack(player.currentPosition)?.let(player::seekTo)
    }

    fun previousTrack() {
        if (hasTracks()) player.seekTo(navigator.previousTrack(player.currentPosition))
    }

    private fun hasTracks(): Boolean = player.trackDurationsMs.isNotEmpty()

    fun nextChapter() {
        if (hasTracks()) navigator.nextChapter(player.currentPosition)?.let(player::seekTo)
    }

    fun previousChapter() {
        if (hasTracks()) player.seekTo(navigator.previousChapter(player.currentPosition))
    }

    fun bookPositionMs(): Long = navigator.bookPositionOf(player.currentPosition)

    fun bookPositionOf(position: TrackPosition): Long = navigator.bookPositionOf(position)

    /** End of the current chapter in book time (sleep timer "End of chapter"). */
    fun currentChapterEndMs(): Long = navigator.chapterEndAt(bookPositionMs())

    /** Media time left in the current track (sleep timer "End of track"); null when its duration is unknown. */
    fun currentTrackRemainingMs(): Long? {
        val position = player.currentPosition
        val duration = player.trackDurationsMs.getOrNull(position.trackIndex)?.takeIf { it > 0L } ?: return null
        return duration - position.positionMs
    }

    fun isChapterStart(bookPositionMs: Long): Boolean = navigator.isChapterStart(bookPositionMs)

    /** Runs the custom command [action]; returns false when the action is unknown. */
    fun execute(action: String, deltaMs: Long, positionMs: Long): Boolean {
        when (action) {
            PlaybackCommands.SEEK_BY -> seekBy(deltaMs)
            PlaybackCommands.SEEK_TO_BOOK_POSITION -> seekToBookPosition(positionMs)
            PlaybackCommands.NEXT_TRACK -> nextTrack()
            PlaybackCommands.PREVIOUS_TRACK -> previousTrack()
            PlaybackCommands.NEXT_CHAPTER -> nextChapter()
            PlaybackCommands.PREVIOUS_CHAPTER -> previousChapter()
            PlaybackCommands.SKIP_BACK -> skipBack()
            PlaybackCommands.SKIP_FORWARD -> skipForward()
            else -> return false
        }
        return true
    }
}
