package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["book_id"])],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "index_in_book") val indexInBook: Int,
    @ColumnInfo(name = "document_uri") val documentUri: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "start_in_book_ms") val startInBookMs: Long,
    @ColumnInfo(name = "title_tag") val titleTag: String?,
    @ColumnInfo(name = "track_number_tag") val trackNumberTag: Int?,
    val unplayable: Boolean,
    @ColumnInfo(name = "metadata_read", defaultValue = "0") val metadataRead: Boolean = false,
)
