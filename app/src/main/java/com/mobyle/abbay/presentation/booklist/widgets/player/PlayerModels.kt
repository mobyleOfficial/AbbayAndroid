package com.mobyle.abbay.presentation.booklist.widgets.player

import com.listening.ShakeSensitivity
import com.model.Book
import com.model.Chapter
import com.player.SleepTimerMode
import com.settings.ProgressLabels
import com.settings.QuickAction

@Suppress("LongParameterList")
class PlayerActions(
    val onPlayButtonClick: (progress: Long) -> Unit,
    val onCoverClick: (progress: Long) -> Unit,
    val onSeekToBookPosition: (bookPositionMs: Long) -> Unit,
    val onSeekBy: (deltaMs: Long) -> Unit,
    val onNextChapter: () -> Unit,
    val onPreviousChapter: () -> Unit,
    val onSpeedChange: (Float) -> Unit,
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
    val routeButton: @androidx.compose.runtime.Composable () -> Unit = {},
)

data class PlayerSkips(
    val smallBackSeconds: Int = 15,
    val smallForwardSeconds: Int = 30,
    val largeBackSeconds: Int = 300,
    val largeForwardSeconds: Int = 300,
)

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

fun progressFraction(bookPositionMs: Long, durationMs: Long): Float = when {
    durationMs <= 0L -> 0f
    durationMs - bookPositionMs <= END_TOLERANCE_MS -> 1f
    else -> (bookPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

private const val END_TOLERANCE_MS = 30_000L

data class PlayerDisplay(
    val progressLabels: ProgressLabels = ProgressLabels.ELAPSED_AND_REMAINING,
    val hiddenQuickActions: Set<QuickAction> = emptySet(),
)

class ChaptersToggle(val isOpen: Boolean = false, val onToggle: () -> Unit = {}, val onClose: () -> Unit = {})

/** Hoisted when given (the chapters can be a pane beside a player pane); in a sheet, open only while the player is expanded. */
internal fun chaptersToggleOf(hoisted: ChaptersToggle?, own: ChaptersToggle, isSheet: Boolean, isExpanded: Boolean): ChaptersToggle = when {
    hoisted == null -> own
    isSheet && !isExpanded -> ChaptersToggle(isOpen = false, onToggle = hoisted.onToggle, onClose = hoisted.onClose)
    else -> hoisted
}
