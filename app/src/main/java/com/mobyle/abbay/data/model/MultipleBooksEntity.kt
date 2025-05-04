package com.mobyle.abbay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class MultipleBooksEntity(
    @PrimaryKey val id: String,
    val bookFileList: String,
    val name: String,
    val thumbnail: String?,
    val progress: Long,
    val duration: Long,
    val currentBookPosition: Int = 0,
    val speed: Float
)