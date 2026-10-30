package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** [date] is the local date as yyyy-MM-dd. */
@Entity(
    tableName = "listening_day",
    primaryKeys = ["book_id", "date"],
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["book_id"]), Index(value = ["date"])],
)
data class ListeningDayEntity(
    @ColumnInfo(name = "book_id") val bookId: Long,
    val date: String,
    @ColumnInfo(name = "book_time_ms") val bookTimeMs: Long,
    @ColumnInfo(name = "wall_time_ms") val wallTimeMs: Long,
    @ColumnInfo(name = "saved_by_speed_ms") val savedBySpeedMs: Long,
    @ColumnInfo(name = "saved_by_silence_ms") val savedBySilenceMs: Long,
)
