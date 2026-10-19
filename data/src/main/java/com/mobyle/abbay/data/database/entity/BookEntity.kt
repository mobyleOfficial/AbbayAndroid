package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [rootId] is null for a single file picked with "Add a file";
 * [relativePath] is then the document URI.
 */
@Entity(
    tableName = "book",
    foreignKeys = [
        ForeignKey(entity = LibraryRootEntity::class, parentColumns = ["id"], childColumns = ["root_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["root_id"]),
        Index(value = ["root_id", "relative_path"], unique = true),
        Index(value = ["sidecar_id"], unique = true),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "root_id") val rootId: Long?,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "folder_uri") val folderUri: String,
    @ColumnInfo(name = "folder_document_id") val folderDocumentId: String,
    val title: String,
    val author: String?,
    val narrator: String?,
    val description: String?,
    @ColumnInfo(name = "cover_uri") val coverUri: String?,
    @ColumnInfo(name = "cover_source") val coverSource: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "is_single_file") val isSingleFile: Boolean,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "last_scanned_at") val lastScannedAt: Long,
    @ColumnInfo(name = "folder_last_modified") val folderLastModified: Long,
    @ColumnInfo(name = "metadata_complete", defaultValue = "0") val metadataComplete: Boolean = false,
    @ColumnInfo(name = "missing_since") val missingSince: Long? = null,
    @ColumnInfo(name = "hidden", defaultValue = "0") val hidden: Boolean = false,
    @ColumnInfo(name = "sidecar_id") val sidecarId: String,
    @ColumnInfo(name = "title_override") val titleOverride: String? = null,
    @ColumnInfo(name = "author_override") val authorOverride: String? = null,
    @ColumnInfo(name = "narrator_override") val narratorOverride: String? = null,
    @ColumnInfo(name = "description_override") val descriptionOverride: String? = null,
)
