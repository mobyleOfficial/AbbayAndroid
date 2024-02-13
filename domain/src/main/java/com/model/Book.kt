package com.model

abstract class Book(
    var name: String,
    val thumbnail: ByteArray?,
    val progress: Long,
    val duration: Long
)