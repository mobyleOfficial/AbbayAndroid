package com.mobyle.abbay.infra.readalong

import android.content.Context
import com.readalong.ReadAlongLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val MODELS_URL = "https://alphacephei.com/vosk/models"

/**
 * The speech model a language is transcribed with: an offline acoustic model the user installs once, on the
 * device, only when the text is first shown for a book in that language.
 *
 * Pinned by size and digest, so a truncated or replaced download fails loudly instead of producing nonsense.
 */
internal enum class ReadAlongModel(
    val language: ReadAlongLanguage,
    val directory: String,
    val url: String,
    val bytes: Long,
    val sha256: String,
) {
    ENGLISH(
        language = ReadAlongLanguage.ENGLISH,
        directory = "vosk-model-small-en-us-0.15",
        url = "$MODELS_URL/vosk-model-small-en-us-0.15.zip",
        bytes = 41_205_931L,
        sha256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498",
    ),
    SPANISH(
        language = ReadAlongLanguage.SPANISH,
        directory = "vosk-model-small-es-0.42",
        url = "$MODELS_URL/vosk-model-small-es-0.42.zip",
        bytes = 39_817_833L,
        sha256 = "09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f",
    ),
    PORTUGUESE(
        language = ReadAlongLanguage.PORTUGUESE,
        directory = "vosk-model-small-pt-0.3",
        url = "$MODELS_URL/vosk-model-small-pt-0.3.zip",
        bytes = 32_453_112L,
        sha256 = "6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7",
    );

    fun matches(candidateBytes: Long, candidateSha256: String): Boolean =
        candidateBytes == bytes && candidateSha256.equals(sha256, ignoreCase = true)

    companion object {
        fun forLanguage(language: ReadAlongLanguage): ReadAlongModel? = entries.firstOrNull { it.language == language }
    }
}

/** Where a language's model sits on disk, and how to get it there. */
@Singleton
@Suppress("NestedBlockDepth", "LoopWithTooManyJumpStatements")
class ReadAlongModelStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val io = Dispatchers.IO

    private val root: File get() = File(context.filesDir, DIRECTORY)

    /** The model's directory, when it is installed and complete. */
    fun installedFor(language: ReadAlongLanguage): File? {
        val model = ReadAlongModel.forLanguage(language) ?: return null
        val directory = File(root, model.directory)
        return directory.takeIf { File(it, "am").isDirectory && File(it, "conf").isDirectory }
    }

    fun isInstalled(language: ReadAlongLanguage): Boolean = installedFor(language) != null

    /**
     * Downloads, checks and unpacks the language's model, reporting progress from 0 to 1. Unpacking happens in a
     * temporary directory that is only renamed into place once it is whole, so an interrupted download never
     * leaves a half model behind.
     */
    suspend fun install(language: ReadAlongLanguage, onProgress: (Float) -> Unit) {
        val model = ReadAlongModel.forLanguage(language) ?: throw ReadAlongEngineException("No model for $language")
        if (isInstalled(language)) return
        withContext(io) {
            val archive = File(context.cacheDir, model.directory + ARCHIVE_SUFFIX)
            try {
                download(model, archive, onProgress)
                unpack(model, archive)
                onProgress(1f)
            } finally {
                archive.delete()
            }
        }
    }

    private suspend fun download(model: ReadAlongModel, target: File, onProgress: (Float) -> Unit) {
        val connection = (URL(model.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw ReadAlongEngineException("Model download failed: HTTP ${connection.responseCode}")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        written += read
                        onProgress((written.toFloat() / model.bytes).coerceIn(0f, 1f))
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!model.matches(written, actual)) {
                throw ReadAlongEngineException("Model download was corrupted ($written bytes, $actual)")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun unpack(model: ReadAlongModel, archive: File) {
        val destination = File(root, model.directory)
        val temporary = File(root, model.directory + TEMPORARY_SUFFIX)
        temporary.deleteRecursively()
        if (!temporary.mkdirs()) throw ReadAlongEngineException("Cannot create ${temporary.path}")
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfter('/', missingDelimiterValue = "")
                // The archive holds one top-level folder; only its contents are written.
                if (name.isEmpty() || !isSafePath(name)) {
                    zip.closeEntry()
                    continue
                }
                val file = File(temporary, name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
        destination.deleteRecursively()
        if (!temporary.renameTo(destination)) {
            throw ReadAlongEngineException("Cannot move the model into place")
        }
    }

    /** No absolute paths and no `..`: an archive must not be able to write outside its own folder. */
    private fun isSafePath(name: String): Boolean {
        val parts = name.split('/')
        return parts.none { it.isEmpty() || it == "." || it == ".." }
    }

    private companion object {
        const val DIRECTORY = "readalong-models"
        const val ARCHIVE_SUFFIX = ".zip"
        const val TEMPORARY_SUFFIX = ".unpacked"
        const val TIMEOUT_MS = 60_000
        const val BUFFER_BYTES = 64 * 1024
    }
}

/** The engine could not do its work: no model, a broken download, a file that isn't audio. */
class ReadAlongEngineException(message: String, cause: Throwable? = null) : Exception(message, cause)
