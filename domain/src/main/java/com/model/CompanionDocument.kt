package com.model

/** A text file next to the audio (.pdf, .epub, .txt, .fb2) that opens in another app. */
data class CompanionDocument(val name: String, val uri: String, val isLastOpened: Boolean)
