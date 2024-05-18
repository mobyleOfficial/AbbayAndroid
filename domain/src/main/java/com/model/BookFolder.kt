package com.model

class BookFolder(
    override val id: String,
    val bookFileList: List<BookFile>,
    override val name: String,
    override val thumbnail: ByteArray?,
    override val progress: Long,
    override val duration: Long
) : Book