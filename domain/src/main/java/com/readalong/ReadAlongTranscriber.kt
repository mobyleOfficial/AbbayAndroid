package com.readalong

import kotlinx.coroutines.flow.Flow

/**
 * The speech engine behind read-along, behind a protocol so the session and the tests can run on a stub and so
 * the engine can be replaced without touching the session.
 */
interface ReadAlongTranscriber {
    /** The languages this build can transcribe, in the order the picker lists them. */
    fun supportedLanguages(): List<ReadAlongLanguage>

    /** Whether the language's model can be used now, after a download, or not at all. */
    suspend fun modelStatus(language: ReadAlongLanguage): ReadAlongModelStatus

    /** Installs the language's model, returning at once when it is already there. */
    suspend fun installModel(language: ReadAlongLanguage)

    /**
     * Transcribes forward from [startMs] (book time), segment after segment, until the end of the book, the
     * first segment not on the device (then finishes), or cancellation. Each chunk carries finished lines and
     * the book-time span now covered.
     */
    fun transcribe(source: ReadAlongAudioSource, fromMs: Long, language: ReadAlongLanguage): Flow<ReadAlongChunk>
}
