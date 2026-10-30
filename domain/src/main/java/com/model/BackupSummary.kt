package com.model

/** Result of a backup export or import. */
data class BackupSummary(
    /** Books written to the file, or books of the file matched in this library. */
    val books: Int,
    /** Books of the file that are not in this library (import only). */
    val unmatchedBooks: Int = 0,
)
