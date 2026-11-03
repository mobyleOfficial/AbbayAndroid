package com.mobyle.abbay.presentation.readalong

import com.readalong.ReadAlongLine

/** The book's sync adjustment as the text's controls show it, and what changes it. */
class ReadAlongSyncControls(
    val offsetMs: Long,
    val onNudge: (steps: Int) -> Unit,
    val onReset: () -> Unit,
    /** Touch and hold on a line: it is the one being read. */
    val onAlign: (ReadAlongLine) -> Unit,
)
