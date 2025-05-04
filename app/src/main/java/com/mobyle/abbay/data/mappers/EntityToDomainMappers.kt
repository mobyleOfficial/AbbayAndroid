package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.BookFile
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
    speed = speed
)

fun BookFileEntity.toDomain() = BookFile(
    id = id,
    name = name,
    fileName = fileName,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration,
    speed = speed
)

private fun String.toEntity() = Json.decodeFromString<BookFileEntity>(this)