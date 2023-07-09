package com.model

class BookFolder(
    val bookFileList: List<BookFile>,
    name: String,
    thumbnail: ByteArray?,
    duration: Int
) : Book(name, thumbnail, duration)