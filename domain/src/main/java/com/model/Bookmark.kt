package com.model

/** A saved place in a book. [chapterTitle] is blank for untitled chapters. */
data class Bookmark(
    val id: Long,
    val bookId: String,
    val bookPositionMs: Long,
    val chapterTitle: String,
    val note: String,
    val createdAt: Long,
) {
    companion object {
        const val MAX_NOTE_LENGTH = 2_000
    }
}
