package com.model

interface Book {
    val id: String
    val name: String
    val thumbnail: ByteArray?
    val progress: Long
    val duration: Long
}

