package com.mobyle.abbay.data.scanner

import com.mobyle.abbay.data.storage.DocumentEntry

/** Structure of one book found on disk, before tags are read. */
data class ScannedBook(
    /** Folder names from the root, joined with `/`; for a loose file, its file name. */
    val relativePath: String,
    val folderDocumentId: String,
    val folderName: String,
    val folderLastModified: Long,
    /** Name of the folder directly above the book, when that folder is not the root (author fallback). */
    val parentFolderName: String?,
    val isSingleFile: Boolean,
    val tracks: List<DocumentEntry>,
    val unsupportedFiles: List<DocumentEntry>,
    val images: List<DocumentEntry>,
    val descriptionFile: DocumentEntry?,
    val cueSheet: DocumentEntry? = null,
    /** The book's `.abbay` folder, when it has one. */
    val sidecarFolderDocumentId: String? = null,
)
