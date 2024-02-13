package com.model

class BookFile(
    val path: String,
    name: String,
    thumbnail: ByteArray?,
    duration: Long
) : Book(name, thumbnail, duration)