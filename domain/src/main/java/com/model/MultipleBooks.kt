package com.model

data class MultipleBooks(
    override val id: String,
    override val name: String,
    override val thumbnail: String?,
    override val progress: Long,
    override val duration: Long,
    override val speed: Float,
    val bookFileList: List<BookFile>,
    val currentBookPosition: Int = 0
) : Book