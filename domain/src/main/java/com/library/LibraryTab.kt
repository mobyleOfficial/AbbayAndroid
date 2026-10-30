package com.library

enum class LibraryTab {
    IN_PROGRESS,
    NEW,
    FINISHED,
    ALL;

    companion object {
        fun ordered(order: List<LibraryTab>): List<LibraryTab> = (order + entries).distinct()

        fun visible(order: List<LibraryTab>, hidden: Set<LibraryTab>): List<LibraryTab> =
            ordered(order).filterNot { it in hidden }.ifEmpty { listOf(ALL) }
    }
}
