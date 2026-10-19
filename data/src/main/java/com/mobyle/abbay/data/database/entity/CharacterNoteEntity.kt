package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_note",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["book_id"])],
)
data class CharacterNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val name: String,
    val description: String,
    @ColumnInfo(name = "first_position_ms") val firstPositionMs: Long?,
)
