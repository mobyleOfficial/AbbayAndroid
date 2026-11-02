package com.repository

import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongTranscript

interface ReadAlongRepository {
    /** The book's transcript; null when there is none, or when the file can't be decoded (then it is deleted). */
    suspend fun transcript(bookId: String): ReadAlongTranscript?

    suspend fun save(bookId: String, transcript: ReadAlongTranscript)

    /** The book left the library, or its text is starting over in another language. */
    suspend fun delete(bookId: String)

    /** Read-along was turned off: nothing it made stays behind. */
    suspend fun deleteAll()

    /** The language picked for the book, when one was. */
    suspend fun language(bookId: String): ReadAlongLanguage?

    suspend fun setLanguage(bookId: String, language: ReadAlongLanguage?)

    /** Where the book's audio is on the device, one segment per file, or null when the book is gone. */
    suspend fun audioSource(bookId: String): ReadAlongAudioSource?
}
