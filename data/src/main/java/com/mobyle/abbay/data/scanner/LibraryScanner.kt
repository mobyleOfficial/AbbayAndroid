package com.mobyle.abbay.data.scanner

import com.library.NaturalOrderComparator
import com.mobyle.abbay.data.storage.DocumentEntry
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.model.DataResult
import javax.inject.Inject

/**
 * Finds books in a SAF tree:
 * - an audio file directly in the root is a one-file book;
 * - a folder with audio files of its own is a book (its sub-folders are scanned as separate books);
 * - a folder without audio whose sub-folders are all disc folders (`CD1`, `Disc 2`…) is one book;
 * - any other folder without audio is a collection and is scanned recursively.
 * Tracks are in natural order. Pure logic over [DocumentTreeSource], so it runs on the JVM in tests.
 */
class LibraryScanner @Inject constructor(private val tree: DocumentTreeSource) {
    private val naturalOrder = NaturalOrderComparator()
    private val byName: Comparator<DocumentEntry> = compareBy(naturalOrder) { FileKinds.baseName(it.name) }

    /** Lists the root once and returns its loose files and its folders. */
    fun listRoot(treeUri: String): DataResult<List<DocumentEntry>> =
        tree.listChildren(treeUri, tree.rootDocumentId(treeUri)).mapEntries { entries -> entries.filterNot(FileKinds::isIgnored) }

    fun looseFileBooks(rootChildren: List<DocumentEntry>): List<ScannedBook> =
        rootChildren.filter(FileKinds::isAudio).sortedWith(byName).map { file ->
            ScannedBook(
                relativePath = file.name,
                folderDocumentId = file.documentId,
                folderName = FileKinds.baseName(file.name),
                folderLastModified = file.lastModified,
                parentFolderName = null,
                isSingleFile = true,
                tracks = listOf(file),
                unsupportedFiles = emptyList(),
                images = emptyList(),
                descriptionFile = null,
            )
        }

    /**
     * Every book at or below [folder]. [knownChildren] avoids listing the folder again when the caller already did.
     * Folders that cannot be listed are skipped.
     */
    fun scanFolder(
        treeUri: String,
        folder: DocumentEntry,
        relativePath: String,
        parentFolderName: String?,
        knownChildren: List<DocumentEntry>? = null,
    ): List<ScannedBook> {
        val children = knownChildren ?: when (val listing = tree.listChildren(treeUri, folder.documentId)) {
            is DataResult.Success -> listing.value
            is DataResult.Failure -> return emptyList()
        }
        val visibleChildren = children.filterNot(FileKinds::isIgnored)
        val audioFiles = visibleChildren.filter(FileKinds::isAudio)
        val subfolders = visibleChildren.filter { it.isDirectory }.sortedWith(byName)
        val sidecarFolderId = children.firstOrNull { it.isDirectory && it.name == FileKinds.SIDECAR_FOLDER }?.documentId

        return when {
            audioFiles.isNotEmpty() -> {
                val book = bookOf(FolderAt(folder, relativePath, parentFolderName), visibleChildren, audioFiles.sortedWith(byName))
                    .copy(sidecarFolderDocumentId = sidecarFolderId)
                listOf(book) + subfolders.flatMap { scanFolder(treeUri, it, "$relativePath/${it.name}", folder.name) }
            }

            subfolders.isNotEmpty() && subfolders.all(FileKinds::isDiscFolder) ->
                listOfNotNull(
                    multiDiscBook(treeUri, FolderAt(folder, relativePath, parentFolderName), visibleChildren, subfolders)
                        ?.copy(sidecarFolderDocumentId = sidecarFolderId),
                )

            else -> subfolders.flatMap { scanFolder(treeUri, it, "$relativePath/${it.name}", folder.name) }
        }
    }

    /** True when [children] make their folder a book (audio of its own, or only disc folders). */
    fun isBookFolder(children: List<DocumentEntry>): Boolean {
        val visibleChildren = children.filterNot(FileKinds::isIgnored)
        val subfolders = visibleChildren.filter { it.isDirectory }
        return visibleChildren.any(FileKinds::isAudio) || (subfolders.isNotEmpty() && subfolders.all(FileKinds::isDiscFolder))
    }

    private fun multiDiscBook(
        treeUri: String,
        folder: FolderAt,
        folderChildren: List<DocumentEntry>,
        discFolders: List<DocumentEntry>,
    ): ScannedBook? {
        val discContents = discFolders.map { disc ->
            when (val listing = tree.listChildren(treeUri, disc.documentId)) {
                is DataResult.Success -> listing.value.filterNot(FileKinds::isIgnored)
                is DataResult.Failure -> emptyList()
            }
        }
        val tracks = discContents.flatMap { contents -> contents.filter(FileKinds::isAudio).sortedWith(byName) }
        if (tracks.isEmpty()) return null

        val allFiles = folderChildren + discContents.flatten()
        return bookOf(folder, allFiles, tracks)
    }

    private fun bookOf(folder: FolderAt, files: List<DocumentEntry>, tracks: List<DocumentEntry>) = ScannedBook(
        relativePath = folder.relativePath,
        folderDocumentId = folder.entry.documentId,
        folderName = folder.entry.name,
        folderLastModified = folder.entry.lastModified,
        parentFolderName = folder.parentFolderName,
        isSingleFile = false,
        tracks = tracks,
        unsupportedFiles = files.filter(FileKinds::isUnsupportedAudio).sortedWith(byName),
        images = files.filter(FileKinds::isImage),
        descriptionFile = files.firstOrNull(FileKinds::isDescription),
        cueSheet = files.firstOrNull(FileKinds::isCueSheet),
    )

    private inline fun DataResult<List<DocumentEntry>>.mapEntries(
        transform: (List<DocumentEntry>) -> List<DocumentEntry>,
    ): DataResult<List<DocumentEntry>> = when (this) {
        is DataResult.Success -> DataResult.Success(transform(value))
        is DataResult.Failure -> this
    }

    /** A folder with its path from the root and the name of the folder above it. */
    private data class FolderAt(val entry: DocumentEntry, val relativePath: String, val parentFolderName: String?)
}
