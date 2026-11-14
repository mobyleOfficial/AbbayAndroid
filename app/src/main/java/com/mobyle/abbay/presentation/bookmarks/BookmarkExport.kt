package com.mobyle.abbay.presentation.bookmarks

import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.Bookmark

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
