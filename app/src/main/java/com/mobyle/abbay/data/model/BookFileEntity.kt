package com.mobyle.abbay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class BookFileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val fileName: String,
    val thumbnail: String?,
    val progress: Long,
    val duration: Long,
    val speed: Float,
    val hasError: Boolean
)