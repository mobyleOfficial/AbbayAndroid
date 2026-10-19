package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_entry",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class QueueEntryEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    val position: Int,
)
