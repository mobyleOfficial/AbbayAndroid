package com.model

class BookFile(
    val id: String,
    name: String,
    thumbnail: ByteArray?,
    progress: Long,
    duration: Long
) : Book(name, thumbnail, progress, duration)