package com.model

/** Read model of a book shown by the preserved AbbayAndroid UI. */
interface Book {
    val id: String
    val name: String
    val thumbnail: String?
    val progress: Long
    val duration: Long
    val speed: Float
    val hasError: Boolean
    val type: BookType
    val author: String?
    val state: BookState
    val addedAt: Long
    val lastPlayedAt: Long?

    /** Position from the start of the book, used for progress and sorting. */
    val bookPositionMs: Long

    /** Chapters in book order; one per file when the files have no chapter information. */
    val chapters: List<Chapter>

    /** True for a file opened from another app and played without being added to the library. */
    val isExternal: Boolean get() = id.startsWith(EXTERNAL_ID_PREFIX)

    /** When playback of this book was last paused; smart rewind measures the pause from it. */
    val lastPausedAt: Long? get() = null

    val narrator: String? get() = null

    /** The library root holding the book, or null for a book added as a single file (folder view). */
    val rootId: Long? get() = null

    /** Folder path of the book inside its root, "/"-separated; blank when it sits at the root's top. */
    val relativePath: String get() = ""

    companion object {
        const val EXTERNAL_ID_PREFIX = "external:"
    }
}
