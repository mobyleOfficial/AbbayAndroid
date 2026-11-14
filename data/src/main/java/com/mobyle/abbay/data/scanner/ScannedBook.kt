package com.mobyle.abbay.data.scanner

import com.mobyle.abbay.data.storage.DocumentEntry

data class ScannedBook(
    val relativePath: String,
    val folderDocumentId: String,
    val folderName: String,
    val folderLastModified: Long,
    val parentFolderName: String?,
    val isSingleFile: Boolean,
    val tracks: List<DocumentEntry>,
    val unsupportedFiles: List<DocumentEntry>,
    val images: List<DocumentEntry>,
    val descriptionFile: DocumentEntry?,
    val cueSheet: DocumentEntry? = null,
    val sidecarFolderDocumentId: String? = null,
)
