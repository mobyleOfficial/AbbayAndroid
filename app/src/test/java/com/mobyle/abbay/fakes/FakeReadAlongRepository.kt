package com.mobyle.abbay.fakes

import com.model.Book
import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongBookText
import com.readalong.ReadAlongEpub
import com.readalong.ReadAlongTranscript
import com.repository.ReadAlongRepository

class FakeReadAlongRepository : ReadAlongRepository {
    val transcripts = mutableMapOf<String, ReadAlongTranscript>()
    val languages = mutableMapOf<String, String>()
    var modelsDeleted = false

    override suspend fun load(bookId: String) = ReadAlongRepository.Loaded(transcripts[bookId], 0)

    override suspend fun save(bookId: String, transcript: ReadAlongTranscript, stamp: Int) {
        transcripts[bookId] = transcript
    }

    override suspend fun delete(bookId: String) {
        transcripts.remove(bookId)
    }

    override suspend fun deleteAll() = transcripts.clear()

    override suspend fun deleteModels() {
        modelsDeleted = true
    }

    override suspend fun language(bookId: String) = languages[bookId]

    override suspend fun setLanguage(bookId: String, locale: String?) {
        if (locale == null) languages.remove(bookId) else languages[bookId] = locale
    }

    val syncOffsets = mutableMapOf<String, Long>()

    override suspend fun syncOffset(bookId: String) = syncOffsets[bookId] ?: 0L

    override suspend fun setSyncOffset(bookId: String, offsetMs: Long) {
        if (offsetMs == 0L) syncOffsets.remove(bookId) else syncOffsets[bookId] = offsetMs
    }

    override suspend fun forgetBooks() {
        languages.clear()
        syncOffsets.clear()
        introSeen.clear()
    }

    override suspend fun audioSource(book: Book) = ReadAlongAudioSource(emptyList(), isDownloadable = false)

    override suspend fun bookText(book: Book): ReadAlongBookText? = null

    override suspend fun epub(book: Book): ReadAlongEpub? = null

    override suspend fun chooseEpub(bookId: String, uri: String?) = true

    val introSeen = mutableSetOf<String>()

    override suspend fun isIntroSeen(bookId: String) = bookId in introSeen

    override suspend fun setIntroSeen(bookId: String, seen: Boolean) {
        if (seen) introSeen += bookId else introSeen -= bookId
    }
}
