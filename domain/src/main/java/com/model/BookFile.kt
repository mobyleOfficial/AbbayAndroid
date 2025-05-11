package com.model

data class BookFile(
    override val id: String,
    override val name: String,
    override val thumbnail: String?,
    override val progress: Long,
    override val duration: Long,
    override val speed: Float,
    override val hasError: Boolean = false,
    val fileName: String
) : Book