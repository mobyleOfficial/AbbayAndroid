package com.readalong

import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks

/** Whether read-along is offered for a book at all: it reads files on the device, so a book only streamed has nothing to read. */
object ReadAlongAvailability {
    /** At least one of the book's files is here: a local book, or one with a chapter downloaded. */
    fun isOffered(book: Book): Boolean = audioUris(book).any { uri -> uri.isNotBlank() && !isRemote(uri) }

    private fun audioUris(book: Book): List<String> = when (book) {
        is MultipleBooks -> book.bookFileList.map { it.uri }
        is BookFile -> listOf(book.uri)
        else -> emptyList()
    }

    private fun isRemote(uri: String) = uri.startsWith("http://") || uri.startsWith("https://")
}
