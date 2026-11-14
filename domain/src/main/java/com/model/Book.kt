package com.model

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

    val bookPositionMs: Long

    val chapters: List<Chapter>

    val isExternal: Boolean get() = id.startsWith(EXTERNAL_ID_PREFIX)

    val lastPausedAt: Long? get() = null

    val narrator: String? get() = null

    val rootId: Long? get() = null

    val relativePath: String get() = ""

    companion object {
        const val EXTERNAL_ID_PREFIX = "external:"
    }
}
