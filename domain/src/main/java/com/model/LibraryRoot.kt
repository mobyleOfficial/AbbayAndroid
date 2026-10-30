package com.model

/** A folder picked by the user; every sub-folder with audio is a book. */
data class LibraryRoot(
    val id: Long,
    val treeUri: String,
    val displayName: String,
    val isAccessible: Boolean,
)
