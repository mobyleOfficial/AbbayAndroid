package com.model

class MultipleBooks(
    override val id: String,
    val bookFileList: List<BookFile>,
    override val name: String,
    override val thumbnail: String?,
    override val progress: Long,
    override val duration: Long
) : Book