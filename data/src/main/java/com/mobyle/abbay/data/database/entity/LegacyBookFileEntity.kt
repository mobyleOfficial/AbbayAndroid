package com.mobyle.abbay.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Version 1 table `BookFileEntity`, renamed by the 1 → 2 migration and dropped after the legacy import. */
@Entity(tableName = "legacy_book_file")
data class LegacyBookFileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fileName: String,
    val thumbnail: String?,
    val progress: Long,
    val duration: Long,
    val speed: Float,
    val hasError: Boolean,
    val type: String,
)
