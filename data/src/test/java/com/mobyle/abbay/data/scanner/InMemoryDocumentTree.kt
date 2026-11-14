package com.mobyle.abbay.data.scanner

import com.mobyle.abbay.data.storage.DocumentEntry
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.model.DataError
import com.model.DataResult

class InMemoryDocumentTree(vararg filePaths: String) : DocumentTreeSource {
    private val children = mutableMapOf<String, MutableList<DocumentEntry>>(ROOT to mutableListOf())
    var listCalls = 0
        private set
    var unreadableFolders = mutableSetOf<String>()

    init {
        filePaths.forEach(::addFile)
    }

    fun addFile(path: String, sizeBytes: Long = 1_000L, lastModified: Long = 1L) {
        val parts = path.split('/')
        var parentId = ROOT
        parts.dropLast(1).forEachIndexed { index, folderName ->
            val folderId = parts.take(index + 1).joinToString("/")
            if (children[parentId].orEmpty().none { it.documentId == folderId }) {
                children.getOrPut(parentId) { mutableListOf() } +=
                    DocumentEntry(folderId, folderName, DocumentEntry.DIRECTORY_MIME_TYPE, lastModified, 0L)
                children.getOrPut(folderId) { mutableListOf() }
            }
            parentId = folderId
        }
        children.getOrPut(parentId) { mutableListOf() } += DocumentEntry(path, parts.last(), "audio/mpeg", lastModified, sizeBytes)
    }

    override fun rootDocumentId(treeUri: String) = ROOT

    override fun rootEntry(treeUri: String): DataResult<DocumentEntry> =
        DataResult.Success(DocumentEntry(ROOT, "Audiobooks", DocumentEntry.DIRECTORY_MIME_TYPE, 1L, 0L))

    override fun listChildren(treeUri: String, parentDocumentId: String): DataResult<List<DocumentEntry>> {
        listCalls++
        if (parentDocumentId in unreadableFolders) return DataResult.Failure(DataError.UNREADABLE)
        return DataResult.Success(children[parentDocumentId].orEmpty().toList())
    }

    override fun documentUri(treeUri: String, documentId: String) = "content://tree/$documentId"

    fun entry(path: String): DocumentEntry = children.values.flatten().first { it.documentId == path }

    companion object {
        const val ROOT = "root"
        const val TREE_URI = "content://tree"
    }
}
