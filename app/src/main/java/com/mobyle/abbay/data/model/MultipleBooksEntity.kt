package com.mobyle.abbay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class MultipleBooksEntity(
    @PrimaryKey var id: String,
    var bookFileList: String,
    var name: String,
    var thumbnail: String?,
    val progress: Long,
    var duration: Long
)