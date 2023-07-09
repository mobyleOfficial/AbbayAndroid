package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookFolderEntity
import com.model.BookFile
import com.model.BookFolder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun BookFolderEntity.toDomain(): BookFolder = BookFolder(
    Json.decodeFromString<List<String>>(this.bookFileList)
        .map {
            it.toEntity().toDomain()
        }, name, thumbnail, duration
)

fun BookFileEntity.toDomain() = BookFile(
    path,
    name,
    thumbnail,
    duration
)

private fun String.toEntity() = Json.decodeFromString<BookFileEntity>(this)