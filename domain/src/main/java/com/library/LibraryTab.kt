package com.library

/** Library tabs. */
enum class LibraryTab {
    IN_PROGRESS,
    NEW,
    FINISHED,
    ALL;

    companion object {
        /**
         * Every tab in the user's order: tabs missing from [order] (added in a later version) go at the end.
         */
        fun ordered(order: List<LibraryTab>): List<LibraryTab> = (order + entries).distinct()

        /** The tabs the library shows, in the user's order; never empty, so All stays when everything is hidden. */
        fun visible(order: List<LibraryTab>, hidden: Set<LibraryTab>): List<LibraryTab> =
            ordered(order).filterNot { it in hidden }.ifEmpty { listOf(ALL) }
    }
}
