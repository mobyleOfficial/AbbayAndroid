package com.mobyle.abbay.data.mappers

import com.mobyle.abbay.data.model.BookFileEntity
import com.mobyle.abbay.data.model.MultipleBooksEntity
import com.model.BookFile
import com.model.MultipleBooks
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

fun MultipleBooksEntity.toDomain(): MultipleBooks = MultipleBooks(
    "",
    Json.decodeFromString<List<String>>(this.bookFileList)
        .map {
            it.toEntity().toDomain()
        }, name, thumbnail, progress, duration
)

fun BookFileEntity.toDomain() = BookFile(
    id = id,
    name = name,
    thumbnail = thumbnail,
    progress = progress,
    duration = duration
)

private fun String.toEntity() = Json.decodeFromString<BookFileEntity>(this)