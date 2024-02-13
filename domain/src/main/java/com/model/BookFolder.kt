package com.model

class BookFolder(
    val bookFileList: List<BookFile>,
    name: String,
    thumbnail: ByteArray?,
    progress: Long,
    duration: Long
) : Book(name, thumbnail, progress, duration)