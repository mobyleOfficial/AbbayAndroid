package com.mobyle.abbay.data.librivox

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogCache @Inject constructor(@ApplicationContext context: Context) {
    private val directory = File(context.cacheDir, DIRECTORY)
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    class Hit<T>(val value: T, val isFresh: Boolean)

    fun <T> read(serializer: KSerializer<T>, key: String, maxAgeMs: Long, now: Long = System.currentTimeMillis()): Hit<T>? =
        synchronized(lock) {
            val text = runCatching { fileOf(key).readText() }.getOrNull() ?: return null
            val entry = runCatching { json.decodeFromString(Entry.serializer(serializer), text) }.getOrNull() ?: return null
            Hit(entry.value, isFresh = now - entry.storedAt < maxAgeMs)
        }

    fun <T> write(serializer: KSerializer<T>, key: String, value: T, now: Long = System.currentTimeMillis()) = synchronized(lock) {
        runCatching {
            directory.mkdirs()
            val temporary = File(directory, "${fileOf(key).name}.tmp")
            temporary.writeText(json.encodeToString(Entry.serializer(serializer), Entry(now, value)))
            temporary.renameTo(fileOf(key))
        }
        Unit
    }

    fun remove(key: String) = synchronized(lock) {
        fileOf(key).delete()
        Unit
    }

    private fun fileOf(key: String) = File(directory, URLEncoder.encode(key, Charsets.UTF_8.name()) + ".json")

    @Serializable
    private class Entry<T>(val storedAt: Long, val value: T)

    private companion object {
        const val DIRECTORY = "librivox_catalog"
    }
}
