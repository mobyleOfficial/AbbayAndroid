package com.model

class BookFolder(
    val bookFileList: List<BookFile>,
    name: String,
    thumbnail: ByteArray?,
    duration: Long
) : Book(name, thumbnail, duration)