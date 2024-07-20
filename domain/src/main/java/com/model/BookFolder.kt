package com.model

data class BookFolder(
    override val id: String,
    override val name: String,
    override val thumbnail: String?,
    override val progress: Long = 0L,
    override val duration: Long = 0L
) : Book