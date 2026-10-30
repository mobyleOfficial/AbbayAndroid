package com.mobyle.abbay.presentation.booklist

import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * What the user chose to see, apart from how it fits: browsing, the player, or the player with its chapters.
 * In one pane the player is the bottom sheet, expanded; split, it is a pane beside browsing and the chapters
 * take browsing's place. A fold, rotation or resize reflows the same choice and never opens the player by itself.
 */
@OptIn(ExperimentalMaterialApi::class)
@Stable
class PlayerPresentation internal constructor(
    private val sheet: BottomSheetState,
    isOpen: Boolean,
    isShowingChapters: Boolean,
) {
    var isOpen by mutableStateOf(isOpen)
        private set
    var isShowingChapters by mutableStateOf(isShowingChapters)
        private set
    internal var layout: PaneLayout by mutableStateOf(PaneLayout.Single)

    suspend fun open() {
        isOpen = true
        if (!layout.isSplit) {
            sheet.expand()
            // A book selected this frame has no peek yet, and the first expand can settle short of it.
            if (!sheet.isExpanded) sheet.expand()
        }
    }

    suspend fun close() {
        isOpen = false
        isShowingChapters = false
        if (!layout.isSplit) sheet.collapse()
    }

    fun setChapters(open: Boolean) {
        isShowingChapters = open && isOpen
    }

    /** In one pane the sheet can also be dragged open or closed. */
    internal fun followSheet(isExpanded: Boolean) {
        isOpen = isExpanded
        if (!isExpanded) isShowingChapters = false
    }

    internal companion object {
        fun saver(sheet: BottomSheetState) = Saver<PlayerPresentation, List<Boolean>>(
            save = { listOf(it.isOpen, it.isShowingChapters) },
            restore = { PlayerPresentation(sheet, isOpen = it[0], isShowingChapters = it[1]) },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberPlayerPresentation(sheet: BottomSheetState, layout: PaneLayout): PlayerPresentation {
    val presentation = rememberSaveable(saver = PlayerPresentation.saver(sheet)) {
        PlayerPresentation(sheet, isOpen = false, isShowingChapters = false)
    }
    SideEffect { presentation.layout = layout }
    LaunchedEffect(presentation, layout.isSplit) {
        if (layout.isSplit) {
            // The pane takes over: the sheet goes back to the mini player, shown only while the pane is closed.
            if (!sheet.isCollapsed) sheet.collapse()
        } else {
            if (presentation.isOpen && !sheet.isExpanded) sheet.expand()
            snapshotFlow { sheet.currentValue }.collect { presentation.followSheet(it == BottomSheetValue.Expanded) }
        }
    }
    return presentation
}
