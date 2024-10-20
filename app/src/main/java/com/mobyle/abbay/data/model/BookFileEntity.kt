package com.mobyle.abbay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class BookFileEntity(
    @PrimaryKey var id: String,
    var name: String,
    var thumbnail: String?,
    val progress: Long,
    var duration: Long
)