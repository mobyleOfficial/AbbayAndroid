package com.converter

data class ConverterOptions(
    val bitrateKbps: Int = DEFAULT_BITRATE,
    val forceMono: Boolean = false,
    val deleteOriginals: Boolean = false,
) {
    companion object {
        const val DEFAULT_BITRATE = 32
        val BITRATES = listOf(24, 32, 48, 64)
    }
}

sealed interface ConversionScope {
    data class Book(val bookId: String) : ConversionScope

    data class Root(val rootId: Long) : ConversionScope
}

data class ConversionPlan(val fileCount: Int, val totalBytes: Long, val estimatedSavingBytes: Long)

enum class ConversionEnd { FINISHED, CANCELLED, BATTERY_LOW, FAILED }

data class ConversionProgress(
    val isRunning: Boolean = false,
    val fileName: String? = null,
    val filesDone: Int = 0,
    val filesTotal: Int = 0,
    val fileFraction: Float = 0f,
    val savedBytes: Long = 0L,
    val estimatedSavingBytes: Long = 0L,
    val failedFiles: Int = 0,
    val end: ConversionEnd? = null,
)

object ConversionEstimate {
    @Suppress("MagicNumber")
    fun opusBytes(durationMs: Long, bitrateKbps: Int): Long = durationMs * bitrateKbps / 8 * 101 / 100

    fun saving(sourceBytes: Long, durationMs: Long, bitrateKbps: Int): Long = (
        sourceBytes - opusBytes(
            durationMs,
            bitrateKbps
        )
        ).coerceAtLeast(0L)
}
