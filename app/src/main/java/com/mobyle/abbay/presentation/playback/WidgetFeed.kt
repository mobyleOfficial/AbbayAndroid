package com.mobyle.abbay.presentation.playback

import androidx.media3.common.Player
import com.mobyle.abbay.presentation.playback.command.PlaybackCommandHandler
import com.mobyle.abbay.presentation.playback.media.BookMediaItems
import com.player.NowPlaying
import com.player.NowPlayingPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps the home screen widgets current: on every change of book, chapter, play
 * state or sleep timer, and every 30 s while playing for the progress bar — not every second.
 */
class WidgetFeed(
    private val player: Player,
    private val commands: PlaybackCommandHandler,
    private val scope: CoroutineScope,
    private val publisher: NowPlayingPublisher,
    private val isSleepTimerRunning: () -> Boolean,
) : Player.Listener {
    private var ticker: Job? = null

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_POSITION_DISCONTINUITY)) {
            publish()
        }
        if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
            ticker?.cancel()
            if (player.isPlaying) {
                ticker = scope.launch {
                    while (isActive) {
                        delay(PROGRESS_INTERVAL_MS)
                        publish()
                    }
                }
            }
        }
    }

    fun publish() {
        val nowPlaying = snapshot()
        scope.launch { publisher.publish(nowPlaying) }
    }

    fun release() {
        ticker?.cancel()
    }

    private fun snapshot(): NowPlaying? {
        val item = player.currentMediaItem ?: return null
        BookMediaItems.bookIdOf(item) ?: return null
        val bookPosition = commands.bookPositionMs()
        val starts = BookMediaItems.chapterStartsOf(item)
        val titles = BookMediaItems.chapterTitlesOf(item)
        val chapterIndex = starts.indexOfLast { it <= bookPosition }
        val chapterStart = starts.getOrNull(chapterIndex) ?: 0L
        val chapterEnd = commands.currentChapterEndMs()
        val progress = if (chapterEnd > chapterStart) (bookPosition - chapterStart).toFloat() / (chapterEnd - chapterStart) else 0f
        return NowPlaying(
            bookTitle = item.mediaMetadata.albumTitle?.toString() ?: item.mediaMetadata.title?.toString().orEmpty(),
            chapterTitle = titles.getOrNull(chapterIndex).orEmpty(),
            coverUri = item.mediaMetadata.artworkUri?.toString(),
            isPlaying = player.isPlaying,
            chapterProgress = progress.coerceIn(0f, 1f),
            isSleepTimerRunning = isSleepTimerRunning(),
        )
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 30_000L
    }
}
