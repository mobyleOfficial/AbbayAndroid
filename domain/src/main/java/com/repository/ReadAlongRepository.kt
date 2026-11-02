package com.repository

import com.model.Book
import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongTranscript

/**
 * Read-along's storage: one transcript per book, kept so what the device transcribed survives a relaunch and is never
 * done twice, and the language each book is read in. Failures to read or write are swallowed: a missing transcript only
 * means the text is made again.
 */
interface ReadAlongRepository {
    /** A transcript read for a session, and the stamp its saves carry, so a book deleted meanwhile isn't written back. */
    data class Loaded(val transcript: ReadAlongTranscript?, val stamp: Int)

    suspend fun load(bookId: String): Loaded

    /** Dropped when the book was deleted since [stamp] was taken. */
    suspend fun save(bookId: String, transcript: ReadAlongTranscript, stamp: Int)

    /** The book left the library, or its text starts over in another language. */
    suspend fun delete(bookId: String)

    /** Every transcript goes: the library was cleared, or Read-along turned off. */
    suspend fun deleteAll()

    /** The downloaded speech models go too: Read-along was turned off, and nothing it fetched stays behind. */
    suspend fun deleteModels()

    suspend fun language(bookId: String): String?

    suspend fun setLanguage(bookId: String, locale: String?)

    /** How far the book's text is moved against its audio, in ms (see [com.readalong.ReadAlongSync]); 0 when never adjusted. */
    suspend fun syncOffset(bookId: String): Long

    /** 0 forgets the adjustment. */
    suspend fun setSyncOffset(bookId: String, offsetMs: Long)

    /** Every book's language and sync adjustment go: the library was cleared. */
    suspend fun forgetBooks()

    /** Where the book's audio is on this device. */
    suspend fun audioSource(book: Book): ReadAlongAudioSource
}
