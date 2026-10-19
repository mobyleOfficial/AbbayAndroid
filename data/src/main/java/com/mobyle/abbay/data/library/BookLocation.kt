package com.mobyle.abbay.data.library

/** Where a scanned book lives: its root (null for a picked file), its folder URI, and how to address its files. */
data class BookLocation(
    val rootId: Long?,
    val folderUri: String,
    val documentUriOf: (documentId: String) -> String,
)
