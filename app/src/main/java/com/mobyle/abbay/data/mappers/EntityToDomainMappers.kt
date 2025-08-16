package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.BookTypeEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.BookFile
import com.model.BookType
import com.model.MultipleBooks
import kotlinx.serialization.json.Json

fun MultipleBooksEntity.toDomain(): MultipleBooks = MultipleBooks(
    id = id,
    bookFileList = Json.decodeFromString<List<String>>(this.bookFileList)
        .map {
            it.toEntity().toDomain()
        },
    name = name,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration,
    currentBookPosition = currentBookPosition,
    speed = speed,
    hasError = hasError,
    type = type.toDomain(),
)

fun BookFileEntity.toDomain() = BookFile(
    id = id,
    name = name,
    fileName = fileName,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration,
    speed = speed,
    hasError = hasError,
    type = type.toDomain(),
)

private fun BookTypeEntity.toDomain(): BookType = when(this) {
    BookTypeEntity.FILE -> BookType.FILE
    else -> BookType.FOLDER
}

private fun String.toEntity() = Json.decodeFromString<BookFileEntity>(this)