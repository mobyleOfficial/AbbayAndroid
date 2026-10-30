package com.mobyle.abbay.data.storage

/** One row of a SAF children query. */
data class DocumentEntry(
    val documentId: String,
    val name: String,
    val mimeType: String,
    val lastModified: Long,
    val sizeBytes: Long,
) {
    val isDirectory: Boolean get() = mimeType == DIRECTORY_MIME_TYPE

    companion object {
        /** Same value as `DocumentsContract.Document.MIME_TYPE_DIR`. */
        const val DIRECTORY_MIME_TYPE = "vnd.android.document/directory"
    }
}
