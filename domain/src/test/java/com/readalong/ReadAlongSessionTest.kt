package com.readalong

import com.model.Book
import com.model.BookFile
import com.model.BookType
import com.repository.ReadAlongRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadAlongSessionTest {
    private val book: Book = BookFile(
        id = "book",
        name = "Book",
        thumbnail = null,
        progress = 0,
        duration = 600_000,
        speed = 1f,
        type = BookType.FILE,
        fileName = "book.mp3",
        uri = "file:/book.mp3",
    )

    private class Run(val startMs: Long, val chunks: Channel<ReadAlongChunk> = Channel(Channel.UNLIMITED))

    private class FakeTranscriber : ReadAlongTranscriber {
        val installed = mutableSetOf("en-US")
        val runs = mutableListOf<Run>()
        var installFails = false
        var installs = 0

        override fun supportedLocales() = listOf("en-US", "pt-BR")

        override suspend fun installedLocales() = installed.sorted()

        override suspend fun modelStatus(locale: String) = when {
            locale in installed -> ReadAlongModelStatus.INSTALLED
            locale in supportedLocales() -> ReadAlongModelStatus.DOWNLOADABLE
            else -> ReadAlongModelStatus.UNSUPPORTED
        }

        override suspend fun installModel(locale: String, onProgress: (Float) -> Unit) {
            installs += 1
            if (installFails) error("offline")
            installed += locale
        }

        override fun transcribe(source: ReadAlongAudioSource, startMs: Long, locale: String): Flow<ReadAlongChunk> {
            val run = Run(startMs)
            runs += run
            return run.chunks.receiveAsFlow()
        }

        override suspend fun confidence(source: ReadAlongAudioSource, startMs: Long, durationMs: Long, locale: String) =
            if (locale == "en-US") 0.9 else 0.2
    }

    private class FakeRepository(var segmentUri: String? = "file:/book.mp3", var isDownloadable: Boolean = false) : ReadAlongRepository {
        val languages = mutableMapOf<String, String>()
        val saved = mutableMapOf<String, ReadAlongTranscript>()
        var deletions = 0

        override suspend fun load(bookId: String) = ReadAlongRepository.Loaded(saved[bookId], deletions)

        override suspend fun save(bookId: String, transcript: ReadAlongTranscript, stamp: Int) {
            if (stamp == deletions) saved[bookId] = transcript
        }

        override suspend fun delete(bookId: String) {
            deletions += 1
            saved.remove(bookId)
        }

        override suspend fun deleteAll() = saved.clear()

        override suspend fun deleteModels() = Unit

        override suspend fun language(bookId: String) = languages[bookId]

        override suspend fun setLanguage(bookId: String, locale: String?) {
            if (locale == null) languages.remove(bookId) else languages[bookId] = locale
        }

        override suspend fun syncOffset(bookId: String) = 0L

        override suspend fun setSyncOffset(bookId: String, offsetMs: Long) = Unit

        override suspend fun forgetBooks() = Unit

        override suspend fun audioSource(book: Book) =
            ReadAlongAudioSource(listOf(ReadAlongAudioSource.Segment(segmentUri, 0, book.duration)), isDownloadable)

        var bookText: ReadAlongBookText? = null

        override suspend fun bookText(book: Book): ReadAlongBookText? = bookText

        override suspend fun epub(book: Book): ReadAlongEpub? = null

        override suspend fun chooseEpub(bookId: String, uri: String?) = true

        val introSeen = mutableSetOf<String>()

        override suspend fun isIntroSeen(bookId: String) = bookId in introSeen

        override suspend fun setIntroSeen(bookId: String, seen: Boolean) {
            if (seen) introSeen += bookId else introSeen -= bookId
        }
    }

    private val transcriber = FakeTranscriber()
    private val repository = FakeRepository().apply { languages["book"] = "en-US" }

    private fun TestScope.session(appLanguage: String = "pt-BR") = ReadAlongSession(
        scope = backgroundScope,
        transcriber = transcriber,
        repository = repository,
        preferredLanguages = { listOf("pt-BR") },
        appLanguage = { appLanguage },
        clock = { testScheduler.currentTime },
    )

    private fun line(startMs: Long, text: String) = ReadAlongLine(
        startMs,
        startMs + 500,
        listOf(ReadAlongWord(text, startMs, startMs + 500))
    )

    @Test
    fun `showing the text transcribes from the shown time and is ready once it is covered`() = runTest {
        val session = session()

        session.show(book, 30_000)
        runCurrent()
        assertEquals(ReadAlongState.Preparing, session.snapshot.value.state)
        assertEquals(listOf(30_000L), transcriber.runs.map { it.startMs })

        transcriber.runs[0].chunks.send(ReadAlongChunk(listOf(line(31_000, "hello")), ReadAlongSpan(30_000, 40_000)))
        runCurrent()

        assertEquals(ReadAlongState.Ready, session.snapshot.value.state)
        assertEquals(listOf("hello"), session.snapshot.value.transcript?.lines?.map { it.text })
        assertEquals("en-US", session.snapshot.value.language)
        assertNotNull(repository.saved["book"])
    }

    @Test
    fun `a seek inside covered audio starts nothing, one past it starts a run there`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()
        transcriber.runs[0].chunks.send(ReadAlongChunk(emptyList(), ReadAlongSpan(0, 200_000)))
        runCurrent()

        session.timeChanged(10_000)
        runCurrent()
        assertEquals(1, transcriber.runs.size)

        session.timeChanged(500_000)
        runCurrent()
        assertEquals(listOf(0L, 500_000L), transcriber.runs.map { it.startMs })
    }

    @Test
    fun `a run stops far enough ahead, and the next starts where it stopped once playback nears it`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()
        val reach = ReadAlongSession.RUN_AHEAD_MS
        transcriber.runs[0].chunks.send(ReadAlongChunk(emptyList(), ReadAlongSpan(0, reach)))
        runCurrent()

        session.timeChanged(reach - ReadAlongSession.LOOKAHEAD_MS - 1)
        runCurrent()
        assertEquals(1, transcriber.runs.size)

        session.timeChanged(reach - ReadAlongSession.LOOKAHEAD_MS + 1)
        runCurrent()
        assertEquals(listOf(0L, reach), transcriber.runs.map { it.startMs })
    }

    @Test
    fun `audio a finished run already read is not read again`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()
        transcriber.runs[0].chunks.send(ReadAlongChunk(emptyList(), ReadAlongSpan(0, 100_000)))
        transcriber.runs[0].chunks.close()
        runCurrent()

        session.timeChanged(90_000)
        runCurrent()

        assertEquals(1, transcriber.runs.size)
    }

    @Test
    fun `a run drops lines in audio an earlier run covered and stops on reaching it`() = runTest {
        repository.saved["book"] = ReadAlongTranscript("en-US", listOf(line(50_000, "old")), listOf(ReadAlongSpan(50_000, 60_000)))
        val session = session()
        session.show(book, 0)
        runCurrent()

        transcriber.runs[0].chunks.send(ReadAlongChunk(listOf(line(40_000, "new"), line(50_010, "dup")), ReadAlongSpan(0, 55_000)))
        runCurrent()

        assertEquals(listOf("new", "old"), session.snapshot.value.transcript?.lines?.map { it.text })
        assertTrue(transcriber.runs[0].chunks.isClosedForReceive || transcriber.runs.size == 1)
    }

    @Test
    fun `nothing starts under the scrubber's finger, the run starts where it lifts`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()
        transcriber.runs[0].chunks.send(ReadAlongChunk(emptyList(), ReadAlongSpan(0, 100_000)))
        runCurrent()

        session.timeChanged(300_000, scrubbing = true)
        session.timeChanged(400_000, scrubbing = true)
        runCurrent()
        assertEquals(1, transcriber.runs.size)

        session.timeChanged(400_000)
        runCurrent()
        assertEquals(400_000L, transcriber.runs.last().startMs)
    }

    @Test
    fun `hiding the text stops the run and saves what it made`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()
        transcriber.runs[0].chunks.send(ReadAlongChunk(listOf(line(1_000, "kept")), ReadAlongSpan(0, 5_000)))
        runCurrent()

        session.stop()
        session.timeChanged(400_000)
        runCurrent()

        assertEquals(1, transcriber.runs.size)
        assertEquals(listOf("kept"), repository.saved["book"]?.lines?.map { it.text })
    }

    @Test
    fun `a missing model is downloaded first`() = runTest {
        repository.languages["book"] = "pt-BR"
        val session = session()

        session.show(book, 0)
        runCurrent()

        assertEquals(1, transcriber.installs)
        assertEquals(1, transcriber.runs.size)
    }

    @Test
    fun `a failed download says so and the next show tries again`() = runTest {
        repository.languages["book"] = "pt-BR"
        transcriber.installFails = true
        val session = session()

        session.show(book, 0)
        runCurrent()
        assertEquals(ReadAlongState.Failed, session.snapshot.value.state)

        transcriber.installFails = false
        session.show(book, 0)
        runCurrent()
        assertEquals(2, transcriber.installs)
        assertEquals(1, transcriber.runs.size)
    }

    @Test
    fun `a language without a model is unsupported`() = runTest {
        repository.languages["book"] = "ja-JP"
        val session = session()

        session.show(book, 0)
        runCurrent()

        assertEquals(ReadAlongState.Unsupported("ja-JP"), session.snapshot.value.state)
    }

    @Test
    fun `a streamed chapter that isn't downloaded needs a download`() = runTest {
        repository.segmentUri = null
        repository.isDownloadable = true
        val session = session()

        session.show(book, 0)
        runCurrent()

        assertEquals(ReadAlongState.NeedsDownload, session.snapshot.value.state)
        assertTrue(transcriber.runs.isEmpty())
    }

    @Test
    fun `without a stored language the installed one that scores best is kept for the book`() = runTest {
        repository.languages.clear()
        transcriber.installed += "pt-BR"
        val session = session()

        session.show(book, 0)
        runCurrent()

        assertEquals("en-US", repository.languages["book"])
        assertEquals("en-US", session.snapshot.value.language)
    }

    @Test
    fun `without any installed model the app's language is used`() = runTest {
        repository.languages.clear()
        transcriber.installed.clear()
        val session = session(appLanguage = "pt-BR")

        session.show(book, 0)
        runCurrent()

        assertEquals("pt-BR", repository.languages["book"])
    }

    @Test
    fun `without a stored language the one the book's EPUB declares wins over detection`() = runTest {
        repository.languages.clear()
        repository.bookText = ReadAlongBookText.of("pt-PT", listOf("Era uma vez"))
        val session = session(appLanguage = "en-US")

        session.show(book, 0)
        runCurrent()

        assertEquals("pt-BR", repository.languages["book"])
        assertEquals("pt-BR", session.snapshot.value.language)
    }

    @Test
    fun `an EPUB in a language without a model leaves the language to detection`() = runTest {
        repository.languages.clear()
        repository.bookText = ReadAlongBookText.of("ja", listOf("むかしむかし"))
        val session = session()

        session.show(book, 0)
        runCurrent()

        assertEquals("en-US", repository.languages["book"])
    }

    @Test
    fun `a bare librivox code is matched to a supported locale`() = runTest {
        repository.languages["book"] = "en"
        val session = session()

        session.show(book, 0)
        runCurrent()

        assertEquals("en-US", repository.languages["book"])
    }

    @Test
    fun `picking a language forgets the text and starts over in it`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()
        transcriber.runs[0].chunks.send(ReadAlongChunk(listOf(line(1_000, "english")), ReadAlongSpan(0, 5_000)))
        runCurrent()

        session.setLanguage("pt-BR")
        runCurrent()

        assertEquals("pt-BR", repository.languages["book"])
        assertNull(repository.saved["book"])
        assertEquals("pt-BR", session.snapshot.value.transcript?.language)
        assertEquals(2, transcriber.runs.size)
    }

    @Test
    fun `a failing run says the text couldn't be made`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()

        transcriber.runs[0].chunks.close(IllegalStateException("broken file"))
        runCurrent()

        assertEquals(ReadAlongState.Failed, session.snapshot.value.state)
    }

    @Test
    fun `turning read-along off forgets everything`() = runTest {
        val session = session()
        session.show(book, 0)
        runCurrent()

        session.disable()
        runCurrent()

        assertEquals(ReadAlongSnapshot(), session.snapshot.value)
    }
}
