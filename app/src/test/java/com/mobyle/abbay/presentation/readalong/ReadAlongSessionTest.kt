package com.mobyle.abbay.presentation.readalong

import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongChunk
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongModelStatus
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongState
import com.readalong.ReadAlongTranscriber
import com.readalong.ReadAlongTranscript
import com.readalong.ReadAlongWord
import com.repository.ReadAlongRepository
import com.usecase.GetReadAlongAudioSource
import com.usecase.GetReadAlongLanguage
import com.usecase.GetReadAlongTranscript
import com.usecase.SaveReadAlongTranscript
import com.usecase.SetReadAlongLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadAlongSessionTest {
    private val bookId = "7"

    private class FakeRepository(
        private val source: ReadAlongAudioSource?,
        var transcript: ReadAlongTranscript? = null,
    ) : ReadAlongRepository {
        var language: ReadAlongLanguage? = null
        val saved = mutableListOf<ReadAlongTranscript>()

        override suspend fun transcript(bookId: String): ReadAlongTranscript? = transcript
        override suspend fun save(bookId: String, transcript: ReadAlongTranscript) {
            saved.add(transcript)
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

        override suspend fun audioSource(bookId: String): ReadAlongAudioSource? = source
    }

    /** A transcriber that hands over its chunks at once and records that it was asked. */
    private class FakeTranscriber(
        private val chunks: List<ReadAlongChunk>,
        private val status: ReadAlongModelStatus = ReadAlongModelStatus.INSTALLED,
    ) : ReadAlongTranscriber {
        var askedFrom: Long? = null
        var isCancelled = false

        override fun supportedLanguages() = ReadAlongLanguage.entries

        override suspend fun modelStatus(language: ReadAlongLanguage) = status

        override suspend fun installModel(language: ReadAlongLanguage) = Unit

        override fun transcribe(source: ReadAlongAudioSource, fromMs: Long, language: ReadAlongLanguage): Flow<ReadAlongChunk> =
            flow {
                askedFrom = fromMs
                try {
                    chunks.forEach { emit(it) }
                } finally {
                    isCancelled = true
                }
            }
    }

    private fun line(startMs: Long, endMs: Long, text: String) =
        ReadAlongLine(startMs, endMs, listOf(ReadAlongWord(text, startMs, endMs)))

    private fun source() = ReadAlongAudioSource(
        listOf(ReadAlongAudioSource.Segment("file:///chapter.mp3", 0, 600_000)),
    )

    private fun dependencies(transcriber: ReadAlongTranscriber, repository: FakeRepository) = ReadAlongDependencies(
        transcriber = transcriber,
        getTranscript = GetReadAlongTranscript(repository),
        saveTranscript = SaveReadAlongTranscript(repository),
        getLanguage = GetReadAlongLanguage(repository),
        setLanguage = SetReadAlongLanguage(repository),
        audioSource = GetReadAlongAudioSource(repository),
    )

    @Test
    fun `showing the text transcribes from the shown time and keeps the lines`() = runTest {
        val transcriber = FakeTranscriber(listOf(ReadAlongChunk(listOf(line(0, 4_000, "alice")), ReadAlongSpan(0, 5_000))))
        val repository = FakeRepository(source())
        val session = ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))

        session.show(bookId, 0)
        advanceUntilIdle()

        assertEquals(0L, transcriber.askedFrom)
        assertEquals(listOf("alice"), session.snapshot().lines.map { it.text })
        assertEquals(ReadAlongState.Ready, session.snapshot().state)
    }

    @Test
    fun `a book whose file is not on the device asks for the download instead of transcribing`() = runTest {
        val transcriber = FakeTranscriber(emptyList())
        val repository = FakeRepository(
            ReadAlongAudioSource(listOf(ReadAlongAudioSource.Segment(null, 0, 600_000)), isRemote = true),
        )
        val session = ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))

        session.show(bookId, 0)
        advanceUntilIdle()

        assertEquals(ReadAlongState.NeedsDownload, session.snapshot().state)
        assertEquals(null, transcriber.askedFrom)
    }

    @Test
    fun `a language this build cannot transcribe is reported, not transcribed`() = runTest {
        val transcriber = FakeTranscriber(emptyList(), status = ReadAlongModelStatus.UNSUPPORTED)
        val repository = FakeRepository(source())
        val session = ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))

        session.show(bookId, 0)
        advanceUntilIdle()

        assertEquals(ReadAlongState.Unsupported(ReadAlongLanguage.ENGLISH), session.snapshot().state)
    }

    @Test
    fun `the text is saved once it is off screen`() = runTest {
        val transcriber = FakeTranscriber(listOf(ReadAlongChunk(listOf(line(0, 4_000, "alice")), ReadAlongSpan(0, 5_000))))
        val repository = FakeRepository(source())
        val session = ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))

        session.show(bookId, 0)
        advanceUntilIdle()
        session.stop()
        advanceUntilIdle()

        assertTrue(repository.saved.isNotEmpty())
        assertEquals(listOf("alice"), repository.saved.last().lines.map { it.text })
    }

    @Test
    fun `a saved transcript in the same language is read back instead of made again`() = runTest {
        val stored = ReadAlongTranscript(ReadAlongLanguage.ENGLISH).apply {
            add(listOf(line(0, 4_000, "stored")), ReadAlongSpan(0, 4_000))
        }
        val transcriber = FakeTranscriber(emptyList())
        val repository = FakeRepository(source(), transcript = stored)
        val session = ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))

        session.show(bookId, 1_000)
        advanceUntilIdle()

        assertEquals(listOf("stored"), session.snapshot().lines.map { it.text })
        // No need to read what is already transcribed: the run carries on after it.
        assertEquals(4_000L, transcriber.askedFrom)
    }

    @Test
    fun `picking a language keeps it for the book and starts its text over`() = runTest {
        val transcriber = FakeTranscriber(listOf(ReadAlongChunk(listOf(line(0, 4_000, "hello")), ReadAlongSpan(0, 5_000))))
        val repository = FakeRepository(source())
        val session = ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))

        session.show(bookId, 0)
        advanceUntilIdle()
        session.setLanguage(ReadAlongLanguage.PORTUGUESE)
        advanceUntilIdle()

        assertEquals(ReadAlongLanguage.PORTUGUESE, repository.language)
        assertEquals(ReadAlongLanguage.PORTUGUESE, session.snapshot().language)
    }

    @Test
    fun `nothing is transcribed before the text is shown`() = runTest {
        val transcriber = FakeTranscriber(listOf(ReadAlongChunk(listOf(line(0, 4_000, "alice")), ReadAlongSpan(0, 5_000))))
        val repository = FakeRepository(source())

        ReadAlongSession(dependencies(transcriber, repository), CoroutineScope(UnconfinedTestDispatcher()))
        advanceUntilIdle()

        assertEquals(null, transcriber.askedFrom)
        assertFalse(repository.saved.isNotEmpty())
    }
}
