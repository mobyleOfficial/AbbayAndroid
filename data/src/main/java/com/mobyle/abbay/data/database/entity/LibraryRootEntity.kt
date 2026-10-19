package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "library_root", indices = [Index(value = ["tree_uri"], unique = true)])
data class LibraryRootEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "tree_uri") val treeUri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "is_accessible", defaultValue = "1") val isAccessible: Boolean = true,
)
