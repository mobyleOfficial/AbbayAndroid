package com.mobyle.abbay.data.model

import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class BookEntity(
    @PrimaryKey val path: String,
    val name: String,
)