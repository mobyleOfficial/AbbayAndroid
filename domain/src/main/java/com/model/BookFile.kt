package com.model

class BookFile(
    val path: String,
    name: String,
    thumbnail: ByteArray?,
    duration: Int
) : Book(name, thumbnail, duration)