package com.readalong

import kotlinx.coroutines.flow.Flow

/** Whether a language's speech model can be used now, after a download, or not at all. */
enum class ReadAlongModelStatus {
    INSTALLED,
    DOWNLOADABLE,
    UNSUPPORTED,
}

/** What a run hands over as it goes: the lines it finished, and the book time now covered, silence included. */
data class ReadAlongChunk(val lines: List<ReadAlongLine>, val covered: ReadAlongSpan)

/**
 * The speech engine behind read-along, behind an interface so the session runs on a fake in tests. Locale ids are
 * BCP-47 with a hyphen ("en-US").
 */
interface ReadAlongTranscriber {
    /** Sorted, so callers that take the first match always pick the same locale. */
    fun supportedLocales(): List<String>

    /** Sorted, as [supportedLocales]. */
    suspend fun installedLocales(): List<String>

    suspend fun modelStatus(locale: String): ReadAlongModelStatus

    /** Downloads the locale's model; returns at once when it is already installed. Throws when it can't. */
    suspend fun installModel(locale: String, onProgress: (Float) -> Unit = {})

    /** Drops what a cancelled [installModel] already fetched for the locale, so it doesn't sit in storage. */
    suspend fun discardModelDownload(locale: String) {}

    /**
     * Transcribes forward from [startMs] (book time), file after file, until the end of the book, the first file not on
     * the device, or cancellation. Each chunk carries finished lines and the span now covered.
     */
    fun transcribe(source: ReadAlongAudioSource, startMs: Long, locale: String): Flow<ReadAlongChunk>

    /** Mean word confidence of [durationMs] of audio from [startMs], for detecting the book's language. */
    suspend fun confidence(source: ReadAlongAudioSource, startMs: Long, durationMs: Long, locale: String): Double

    /** Frees what the engine holds in memory (a loaded model) once no text is on screen. */
    fun release() {}
}

class UnsupportedReadAlongLocaleException(locale: String) : IllegalArgumentException("No speech model for $locale")
