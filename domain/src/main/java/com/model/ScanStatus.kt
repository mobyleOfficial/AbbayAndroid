package com.model

/** Progress of a library scan, shown as a linear indicator below the top bar. */
data class ScanStatus(
    val isScanning: Boolean = false,
    val processedBooks: Int = 0,
    val totalBooks: Int = 0,
)
