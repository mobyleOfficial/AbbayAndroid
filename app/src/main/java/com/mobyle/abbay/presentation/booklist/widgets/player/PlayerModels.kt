package com.mobyle.abbay.presentation.booklist.widgets.player

import com.listening.ShakeSensitivity
import com.model.Book
import com.model.Chapter
import com.player.SleepTimerMode
import com.settings.ProgressLabels
import com.settings.QuickAction

/** Playback intents of the player screen; the ViewModel implements them. */
@Suppress("LongParameterList") // A holder of callbacks: grouping them is the point.
class PlayerActions(
    val onPlayButtonClick: (progress: Long) -> Unit,
    val onCoverClick: (progress: Long) -> Unit,
    val onSeekToBookPosition: (bookPositionMs: Long) -> Unit,
    val onSeekBy: (deltaMs: Long) -> Unit,
    val onNextChapter: () -> Unit,
    val onPreviousChapter: () -> Unit,
    /** Applies and saves a speed chosen by the user. */
    val onSpeedChange: (Float) -> Unit,
    /** Applies the book's saved speed when the book is opened. */
    val onApplyBookSpeed: (Float) -> Unit,
    val onSetDefaultSpeed: (Float) -> Unit,
    val onTrackChanged: () -> Unit,
    val onLockScreen: (Boolean) -> Unit,
    val onOpenDetails: () -> Unit,
    val onMarkFinished: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onAddBookmark: () -> Unit = {},
    val onStartSleepTimer: (SleepTimerMode) -> Unit = {},
    val onCancelSleepTimer: () -> Unit = {},
    val onShakeSensitivityChange: (ShakeSensitivity) -> Unit = {},
    val onOpenBookmarks: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onOpenCharacters: () -> Unit = {},
    val onOpenStatistics: () -> Unit = {},
    val onOpenBookText: () -> Unit = {},
    val onOpenText: () -> Unit = {},
    /** The Cast route button, or nothing (hidden in settings, or a build without Cast). */
    val routeButton: @androidx.compose.runtime.Composable () -> Unit = {},
)

/** Skip amounts from the settings. */
data class PlayerSkips(
    val smallBackSeconds: Int = 15,
    val smallForwardSeconds: Int = 30,
    val largeBackSeconds: Int = 300,
    val largeForwardSeconds: Int = 300,
)

/** Where the book is: current chapter and positions in book time. */
data class PlayerPosition(val bookPositionMs: Long, val chapter: Chapter?, val chapterCount: Int) {
    val chapterElapsedMs: Long get() = chapter?.let { bookPositionMs - it.startMs }?.coerceAtLeast(0L) ?: bookPositionMs
    val chapterDurationMs: Long get() = chapter?.let { it.endMs - it.startMs }?.coerceAtLeast(1L) ?: 1L

    companion object {
        fun of(book: Book): PlayerPosition {
            val position = book.bookPositionMs
            val chapter = book.chapters.lastOrNull { it.startMs <= position } ?: book.chapters.firstOrNull()
            return PlayerPosition(position, chapter, book.chapters.size)
        }
    }
}

/** A book within 30 s of its end counts as 100 %. */
fun progressFraction(bookPositionMs: Long, durationMs: Long): Float = when {
    durationMs <= 0L -> 0f
    durationMs - bookPositionMs <= END_TOLERANCE_MS -> 1f
    else -> (bookPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

private const val END_TOLERANCE_MS = 30_000L

/** How the player shows its labels and which quick actions it hides (Settings). */
data class PlayerDisplay(
    val progressLabels: ProgressLabels = ProgressLabels.ELAPSED_AND_REMAINING,
    val hiddenQuickActions: Set<QuickAction> = emptySet(),
)
