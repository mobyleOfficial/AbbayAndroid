package com.mobyle.abbay.presentation.readalong

import com.readalong.ReadAlongEpub

/** The EPUB the text comes from, as the text's menu shows it, and the picker to use another (when the folder has none). */
class ReadAlongBookTextControls(
    val epub: ReadAlongEpub?,
    val onChoose: () -> Unit,
    val onRemove: () -> Unit,
)
