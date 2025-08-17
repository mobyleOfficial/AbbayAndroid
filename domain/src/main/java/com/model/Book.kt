package com.model

interface Book {
    val id: String
    val name: String
    val thumbnail: String?
    val progress: Long
    val duration: Long
    val speed: Float
    val hasError: Boolean
    val type: BookType
}
