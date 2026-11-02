package com.mobyle.abbay.fakes

import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongTranscript
import com.repository.ReadAlongRepository

class FakeReadAlongRepository : ReadAlongRepository {
    var transcript: ReadAlongTranscript? = null
    var language: ReadAlongLanguage? = null
    var audioSource: ReadAlongAudioSource? = null
    val saved = mutableListOf<ReadAlongTranscript>()

    override suspend fun transcript(bookId: String): ReadAlongTranscript? = transcript

    override suspend fun save(bookId: String, transcript: ReadAlongTranscript) {
        saved.add(transcript)
        this.transcript = transcript
    }

    override suspend fun delete(bookId: String) {
        transcript = null
    }

    override suspend fun deleteAll() {
        transcript = null
    }

    override suspend fun language(bookId: String): ReadAlongLanguage? = language

    override suspend fun setLanguage(bookId: String, language: ReadAlongLanguage?) {
        this.language = language
    }

    override suspend fun audioSource(bookId: String): ReadAlongAudioSource? = audioSource
}
