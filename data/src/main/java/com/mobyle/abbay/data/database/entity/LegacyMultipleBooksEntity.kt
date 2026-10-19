package com.mobyle.abbay.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Version 1 table `MultipleBooksEntity`, renamed by the 1 → 2 migration and dropped after the legacy import. */
@Entity(tableName = "legacy_multiple_books")
data class LegacyMultipleBooksEntity(
    @PrimaryKey val id: String,
    val bookFileList: String,
    val name: String,
    val thumbnail: String?,
    val progress: Long,
    val duration: Long,
    val currentBookPosition: Int,
    val speed: Float,
    val hasError: Boolean,
    val type: String,
)
