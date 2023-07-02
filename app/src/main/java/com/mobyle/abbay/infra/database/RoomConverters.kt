package com.mobyle.abbay.infra.database

import androidx.room.TypeConverter
import com.mobyle.abbay.data.model.BookEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections

class RoomConverters {
    @TypeConverter
    fun stringToBooksList(data: String?): List<BookEntity?> =
        data?.let { Json.decodeFromString<List<BookEntity>>(it) } ?: Collections.emptyList()

    @TypeConverter
    fun booksToString(list: List<BookEntity?>): String = list.let { Json.encodeToString(list) }
}