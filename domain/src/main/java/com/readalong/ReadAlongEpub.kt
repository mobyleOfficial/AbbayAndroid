package com.readalong

/**
 * The EPUB whose text read-along shows for a book: one in the audiobook's own folder, which is used by default, or,
 * when there is none, one the listener chose from anywhere.
 */
data class ReadAlongEpub(val uri: String, val name: String, val isInBookFolder: Boolean)
