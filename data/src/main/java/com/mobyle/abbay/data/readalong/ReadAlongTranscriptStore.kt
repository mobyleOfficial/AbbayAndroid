package com.mobyle.abbay.data.readalong

import android.content.Context
import com.readalong.ReadAlongTranscript
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-along transcripts on disk, one JSON file per book, so what the device has already transcribed survives a
 * relaunch and is never done twice.
 *
 * Files are named after a SHA-256 of the book's id, so no title is readable from the directory. Everything runs
 * on the IO dispatcher behind one mutex, so the transcriber, the player and Settings can touch it from any
 * thread. Every failure to read or write is swallowed: a missing transcript just means the text is made again.
 */
@Singleton
class ReadAlongTranscriptStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val io = Dispatchers.IO
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private val root: File get() = File(context.filesDir, DIRECTORY)

    /** The book's transcript; null when there is none, or when the file can't be decoded (then it is deleted). */
    suspend fun read(bookId: String): ReadAlongTranscript? = withContext(io) {
        mutex.withLock {
            val file = fileFor(bookId)
            val text = runCatching { file.readText() }.getOrNull() ?: return@withLock null
            runCatching { json.decodeFromString(ReadAlongTranscriptFile.serializer(), text).toTranscript() }
                .getOrElse {
                    file.delete()
                    null
                }
        }
    }

    suspend fun write(bookId: String, transcript: ReadAlongTranscript): Unit = withContext(io) {
        mutex.withLock {
            val text = runCatching { json.encodeToString(ReadAlongTranscriptFile.serializer(), ReadAlongTranscriptFile.of(transcript)) }
                .getOrNull() ?: return@withLock
            runCatching {
                val root = root
                if (!root.isDirectory && !root.mkdirs()) return@withLock
                val target = fileFor(bookId)
                val temporary = File(root, target.name + TEMPORARY_SUFFIX)
                temporary.writeText(text)
                if (!temporary.renameTo(target)) {
                    target.writeText(text)
                    temporary.delete()
                }
            }
        }
    }

    suspend fun delete(bookId: String): Unit = withContext(io) {
        mutex.withLock { fileFor(bookId).delete() }
        Unit
    }

    /** Read-along was turned off: nothing it made stays behind. */
    suspend fun deleteAll(): Unit = withContext(io) {
        mutex.withLock { root.deleteRecursively() }
        Unit
    }

    private fun fileFor(bookId: String): File = File(root, digest(bookId) + EXTENSION)

    private fun digest(bookId: String): String =
        MessageDigest.getInstance("SHA-256").digest(bookId.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val DIRECTORY = "readalong"
        const val EXTENSION = ".json"
        const val TEMPORARY_SUFFIX = ".tmp"
    }
}
