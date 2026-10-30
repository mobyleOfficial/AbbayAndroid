package com.model

data class BackupSummary(
    val books: Int,
    val unmatchedBooks: Int = 0,
)
