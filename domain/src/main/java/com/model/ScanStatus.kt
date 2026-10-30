package com.model

data class ScanStatus(
    val isScanning: Boolean = false,
    val processedBooks: Int = 0,
    val totalBooks: Int = 0,
)
