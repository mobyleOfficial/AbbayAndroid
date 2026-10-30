package com.converter

/** Options of the MP3 → Opus converter. */
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

/** One book, or every book of a library root. */
sealed interface ConversionScope {
    data class Book(val bookId: String) : ConversionScope

    data class Root(val rootId: Long) : ConversionScope
}

/** What a scope holds before converting: MP3 files and their size, and an estimate of what Opus saves. */
data class ConversionPlan(val fileCount: Int, val totalBytes: Long, val estimatedSavingBytes: Long)

enum class ConversionEnd { FINISHED, CANCELLED, BATTERY_LOW, FAILED }

/** The converter's state, shown on its screen and in its notification. */
data class ConversionProgress(
    val isRunning: Boolean = false,
    val fileName: String? = null,
    val filesDone: Int = 0,
    val filesTotal: Int = 0,
    /** Progress inside the current file, 0…1. */
    val fileFraction: Float = 0f,
    /** Bytes saved by this run so far. */
    val savedBytes: Long = 0L,
    val estimatedSavingBytes: Long = 0L,
    val failedFiles: Int = 0,
    val end: ConversionEnd? = null,
)

object ConversionEstimate {
    /** Opus at [bitrateKbps] for audio that is [durationMs] long, plus about 1 % of Ogg overhead. */
    @Suppress("MagicNumber") // Bits per byte, milliseconds per second and the 1 % overhead.
    fun opusBytes(durationMs: Long, bitrateKbps: Int): Long = durationMs * bitrateKbps / 8 * 101 / 100

    fun saving(sourceBytes: Long, durationMs: Long, bitrateKbps: Int): Long = (
        sourceBytes - opusBytes(
            durationMs,
            bitrateKbps
        )
        ).coerceAtLeast(0L)
}
