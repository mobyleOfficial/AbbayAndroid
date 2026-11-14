package com.mobyle.abbay.data.library

data class BookLocation(
    val rootId: Long?,
    val folderUri: String,
    val documentUriOf: (documentId: String) -> String,
)
