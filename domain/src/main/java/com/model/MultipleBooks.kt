package com.model

data class MultipleBooks(
    override val id: String,
    override val name: String,
    override val thumbnail: String?,
    override val progress: Long,
    override val duration: Long,
    override val speed: Float,
    override val hasError: Boolean = false,
    override val type: BookType,
    val bookFileList: List<BookFile>,
    val currentBookPosition: Int = 0,
    override val author: String? = null,
    override val state: BookState = BookState.NEW,
    override val addedAt: Long = 0L,
    override val lastPlayedAt: Long? = null,
    override val chapters: List<Chapter> = emptyList(),
    override val lastPausedAt: Long? = null,
    override val narrator: String? = null,
    override val rootId: Long? = null,
    override val relativePath: String = "",
) : Book {
    override val bookPositionMs: Long
        get() = bookFileList.take(currentBookPosition).sumOf { it.duration } + progress
}
