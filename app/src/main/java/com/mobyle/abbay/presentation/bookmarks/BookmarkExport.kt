package com.mobyle.abbay.presentation.bookmarks

import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Bookmark

/** Plain-text form of a book's bookmarks: a title line, then position, chapter and note of each bookmark. */
object BookmarkExport {
    fun toText(title: String, bookmarks: List<Bookmark>): String = buildString {
        appendLine(title)
        bookmarks.sortedBy { it.bookPositionMs }.forEach { bookmark ->
            appendLine()
            append(bookmark.bookPositionMs.toHHMMSS())
            if (bookmark.chapterTitle.isNotBlank()) append(" — ").append(bookmark.chapterTitle)
            appendLine()
            if (bookmark.note.isNotBlank()) appendLine(bookmark.note)
        }
    }.trimEnd()
}
