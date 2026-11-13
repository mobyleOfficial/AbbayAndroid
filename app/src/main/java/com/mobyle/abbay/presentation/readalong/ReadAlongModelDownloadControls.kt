package com.mobyle.abbay.presentation.readalong

/** The speech model's download, as the text's status offers it: stop it while it runs, or start it again once stopped. */
class ReadAlongModelDownloadControls(
    val onCancel: () -> Unit,
    val onRetry: () -> Unit,
)
