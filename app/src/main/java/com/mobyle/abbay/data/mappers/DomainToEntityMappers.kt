package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookTypeEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.BookFile
import com.model.BookType
import com.model.MultipleBooks
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun MultipleBooks.toEntity(): MultipleBooksEntity = MultipleBooksEntity(
    id = bookFileList.first().id,
    bookFileList = Json.encodeToString(
        bookFileList.map {
            it.toEntity().toJson()
        }
    ),
    name = name,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration,
    currentBookPosition = currentBookPosition,
    speed = speed,
    hasError = hasError,
    type = type.toEntity()
)

fun BookFile.toEntity() = BookFileEntity(
    id = id,
    name = name,
    fileName = fileName,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration,
    speed = speed,
    hasError = hasError,
    type = type.toEntity()
)

private fun BookType.toEntity(): BookTypeEntity = when (this) {
    BookType.FILE -> BookTypeEntity.FILE
    else -> BookTypeEntity.FOLDER
}

private fun BookFileEntity.toJson() = Json.encodeToString(this)