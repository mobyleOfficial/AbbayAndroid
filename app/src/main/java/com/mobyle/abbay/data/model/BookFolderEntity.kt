package com.mobyle.abbay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class BookFolderEntity(
    @PrimaryKey var path: String,
    var bookFileList: String,
    var name: String,
    var thumbnail: ByteArray?,
    var duration: Long
)