package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity
import com.model.BookFile
import com.model.BookFolder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun BookFolder.toEntity(): BookFolderEntity = BookFolderEntity(
    bookFileList.first().path,
    Json.encodeToString(bookFileList.map {
        it.toEntity().toJson()
    }
    ), name, thumbnail, duration
)

fun BookFile.toEntity() = BookFileEntity(
    path,
    name,
    thumbnail,
    duration
)

private fun BookFileEntity.toJson() = Json.encodeToString(this)