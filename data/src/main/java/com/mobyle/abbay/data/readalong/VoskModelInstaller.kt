package com.mobyle.abbay.data.readalong

import android.content.Context
import com.mobyle.abbay.data.librivox.HostAccess
import com.mobyle.abbay.data.librivox.LibriVoxHttp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The speech models on the device, one folder per locale in the app's no-backup storage. A model is downloaded once,
 * over https from its publisher only, checked against the size and SHA-256 pinned in [VoskModels] before anything is
 * unpacked, and unpacked next to its final place, then moved there whole: a folder with the ready mark is complete.
 */
@Singleton
class VoskModelInstaller internal constructor(
    private val root: File,
    private val downloads: File,
    private val http: LibriVoxHttp,
) {
    @Inject
    constructor(@ApplicationContext context: Context, http: LibriVoxHttp) : this(
        File(context.noBackupFilesDir, MODELS_DIRECTORY),
        File(context.cacheDir, MODELS_DIRECTORY),
        http,
    )

    private val lock = Mutex()

    fun installedLocales(): List<String> = VoskModels.all.map { it.locale }.filter(::isInstalled).sorted()

    fun isInstalled(locale: String): Boolean = File(folder(locale), READY_MARK).exists()

    /** The folder the engine loads, once installed. */
    fun folder(locale: String): File = File(root, locale)

    suspend fun install(locale: String, onProgress: (Float) -> Unit): Unit = lock.withLock {
        if (isInstalled(locale)) return@withLock
        val model = VoskModels.forLocale(locale) ?: throw IOException("No model for $locale")
        val context = currentCoroutineContext()
        withContext(Dispatchers.IO) {
            downloads.mkdirs()
            val archive = File(downloads, model.archive)
            http.download(model.url, archive, isStopped = { !context.isActive }, access = Publisher) { received, _ ->
                onProgress((received.toFloat() / model.sizeBytes).coerceIn(0f, 1f))
            }
            try {
                verify(archive, model)
                unpack(archive, model)
            } finally {
                archive.delete()
            }
        }
    }

    /** Every model goes: Read-along was turned off. */
    suspend fun deleteAll() {
        lock.withLock {
            withContext(Dispatchers.IO) {
                root.deleteRecursively()
                downloads.deleteRecursively()
            }
        }
    }

    private fun verify(archive: File, model: VoskModels.Model) {
        ensure(archive.length() == model.sizeBytes) { "Unexpected size for ${model.archive}" }
        val digest = MessageDigest.getInstance("SHA-256")
        archive.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var count = input.read(buffer)
            while (count >= 0) {
                digest.update(buffer, 0, count)
                count = input.read(buffer)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        ensure(hash.equals(model.sha256, ignoreCase = true)) { "Checksum mismatch for ${model.archive}" }
    }

    /** Unpacks the archive next to the models, then moves the model's own folder into place whole. */
    private fun unpack(archive: File, model: VoskModels.Model) {
        val staging = File(root, ".${model.locale}.staging")
        staging.deleteRecursively()
        staging.mkdirs()
        unzip(archive, staging)
        val unpacked = File(staging, model.folder)
        ensure(unpacked.isDirectory) { "Missing ${model.folder} in ${model.archive}" }
        VoskModelLayout.modernise(unpacked)
        File(unpacked, READY_MARK).writeText(model.archive)
        val destination = folder(model.locale)
        destination.deleteRecursively()
        ensure(unpacked.renameTo(destination)) { "Could not move ${model.folder}" }
        staging.deleteRecursively()
    }

    /** Refuses any entry that would land outside [into], and an archive that unpacks to more than [MAX_UNPACKED_BYTES]. */
    private fun unzip(archive: File, into: File) {
        val intoPath = into.canonicalPath + File.separator
        var unpackedBytes = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val target = File(into, entry.name)
                ensure(target.canonicalPath.startsWith(intoPath)) { "Unsafe entry ${entry.name}" }
                unpackedBytes += extract(zip, entry.isDirectory, target)
                ensure(unpackedBytes <= MAX_UNPACKED_BYTES) { "Model too large" }
            }
        }
    }

    /** Writes the current entry to [target]; the bytes written. */
    private fun extract(zip: ZipInputStream, isDirectory: Boolean, target: File): Long {
        if (isDirectory) {
            target.mkdirs()
            return 0L
        }
        target.parentFile?.mkdirs()
        return target.outputStream().use { output -> zip.copyTo(output) }
    }

    private inline fun ensure(condition: Boolean, message: () -> String) {
        if (!condition) throw IOException(message())
    }

    /** The models' publisher, over https: no other host is reached, redirects included. */
    private object Publisher : HostAccess {
        override fun isAllowed(url: URL) = url.protocol == "https" && url.host.lowercase() == VoskModels.HOST
    }

    private companion object {
        const val MODELS_DIRECTORY = "readalong-models"
        const val READY_MARK = ".ready"
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_UNPACKED_BYTES = 400L * 1024 * 1024
    }
}
