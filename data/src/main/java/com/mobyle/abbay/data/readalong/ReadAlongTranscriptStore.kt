package com.mobyle.abbay.data.readalong

import android.content.Context
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongTranscript
import com.readalong.ReadAlongWord
import com.repository.ReadAlongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-along transcripts on disk, one JSON file per book, so what the device transcribed survives a relaunch. Files
 * are named after a SHA-256 of the book's id, so no title is readable from the folder, which lives in the app's
 * no-backup storage: a transcript is derived from audio the device already holds.
 *
 * Every failure to read or write is swallowed: a missing transcript only means the text is made again. All access is
 * serialised by one lock taken in call order, so a save asked for before [deleteAll] is removed by it.
 */
@Singleton
class ReadAlongTranscriptStore internal constructor(private val root: File) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.noBackupFilesDir, DIRECTORY))

    private val lock = Mutex()

    /** How often each book's file was deleted, by file name: saves stamped before a deletion are dropped. */
    private val deletions = HashMap<String, Int>()

    suspend fun load(bookId: String): ReadAlongRepository.Loaded = locked {
        val name = fileName(bookId)
        ReadAlongRepository.Loaded(read(File(root, name)), deletions[name] ?: 0)
    }

    suspend fun save(bookId: String, transcript: ReadAlongTranscript, stamp: Int): Unit = locked {
        val name = fileName(bookId)
        if ((deletions[name] ?: 0) != stamp) return@locked
        runCatching {
            root.mkdirs()
            val target = File(root, name)
            val partial = File(root, "$name.part")
            partial.writeText(json.encodeToString(TranscriptFile.serializer(), TranscriptFile.of(transcript)))
            if (!partial.renameTo(target)) partial.delete()
        }
        Unit
    }

    suspend fun delete(bookId: String): Unit = locked {
        val name = fileName(bookId)
        deletions[name] = (deletions[name] ?: 0) + 1
        File(root, name).delete()
        Unit
    }

    suspend fun deleteAll(): Unit = locked {
        root.listFiles()?.forEach { file -> deletions[file.name] = (deletions[file.name] ?: 0) + 1 }
        root.deleteRecursively()
        Unit
    }

    private suspend fun <T> locked(block: () -> T): T = lock.withLock { withContext(Dispatchers.IO) { block() } }

    /** Nil when there is no file or it can't be decoded; then it is deleted, so a bad file doesn't linger. */
    private fun read(file: File): ReadAlongTranscript? {
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(TranscriptFile.serializer(), file.readText()).toDomain() }
            .onFailure { file.delete() }
            .getOrNull()
    }

    private fun fileName(bookId: String): String =
        MessageDigest.getInstance("SHA-256").digest(bookId.toByteArray()).joinToString("") { "%02x".format(it) } + ".json"

    @Serializable
    internal data class TranscriptFile(
        @SerialName("v") val version: Int = VERSION,
        @SerialName("language") val language: String,
        @SerialName("lines") val lines: List<LineFile>,
        @SerialName("covered") val covered: List<List<Long>>,
    ) {
        fun toDomain(): ReadAlongTranscript? {
            if (version != VERSION) return null
            return ReadAlongTranscript(
                language = language,
                lines = lines.map(LineFile::toDomain),
                covered = covered.filter { it.size == 2 }.map { ReadAlongSpan(it[0], it[1]) },
            )
        }

        companion object {
            fun of(transcript: ReadAlongTranscript) = TranscriptFile(
                language = transcript.language,
                lines = transcript.lines.filter { it.words.isNotEmpty() }.map(LineFile::of),
                covered = transcript.covered.map { listOf(it.startMs, it.endMs) },
            )
        }
    }

    /** A line's words, each as [text, startMs, endMs] packed into parallel arrays: a whole book's text stays small. */
    @Serializable
    internal data class LineFile(
        @SerialName("w") val words: List<String>,
        @SerialName("s") val starts: List<Long>,
        @SerialName("e") val ends: List<Long>,
    ) {
        fun toDomain(): ReadAlongLine {
            val parsed = words.indices.map { index -> ReadAlongWord(words[index], starts[index], ends[index]) }
            return ReadAlongLine(parsed.first().startMs, parsed.last().endMs, parsed)
        }

        companion object {
            fun of(line: ReadAlongLine) = LineFile(
                words = line.words.map { it.text },
                starts = line.words.map { it.startMs },
                ends = line.words.map { it.endMs },
            )
        }
    }

    private companion object {
        const val DIRECTORY = "readalong"
        const val VERSION = 1
        val json = Json { ignoreUnknownKeys = true }
    }
}
