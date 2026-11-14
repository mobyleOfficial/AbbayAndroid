package com.mobyle.abbay.data.storage

import com.model.DataResult

interface DocumentTreeSource {
    fun rootDocumentId(treeUri: String): String

    fun rootEntry(treeUri: String): DataResult<DocumentEntry>

    fun listChildren(treeUri: String, parentDocumentId: String): DataResult<List<DocumentEntry>>

    fun documentUri(treeUri: String, documentId: String): String
}
