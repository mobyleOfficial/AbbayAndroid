package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity
import com.model.BookFile
import com.model.BookFolder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun BookFolder.toEntity(): BookFolderEntity = BookFolderEntity(
    bookFileList.first().id,
    Json.encodeToString(bookFileList.map {
        it.toEntity().toJson()
    }
    ), name, thumbnail, progress, duration
)

fun BookFile.toEntity() = BookFileEntity(
    id = id,
    name = name,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration
)

private fun BookFileEntity.toJson() = Json.encodeToString(this)