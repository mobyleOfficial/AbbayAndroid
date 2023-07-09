package com.mobyle.abbay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class BookEntity(
    @PrimaryKey val path: String,
    val name: String,
    val thumbnail: ByteArray?,
    val duration: Int
)