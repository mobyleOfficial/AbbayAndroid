package com.mobyle.abbay.data.librivox

import android.content.Context
import android.net.Uri
import com.mobyle.abbay.data.database.dao.LibraryDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibriVoxFiles @Inject constructor(@ApplicationContext context: Context, private val dao: LibraryDao) {
    private val root = File(context.filesDir, DIRECTORY)

    fun fileFor(librivoxId: String, remoteUrl: String): File? {
        val name = remoteUrl.substringBefore('?').substringAfterLast('/')
        if (!isSafeName(librivoxId) || !isSafeName(name)) return null
        return File(File(root, librivoxId), name)
    }

    fun delete(librivoxId: String) {
        if (isSafeName(librivoxId)) File(root, librivoxId).deleteRecursively()
    }

    fun deleteAll() {
        root.deleteRecursively()
    }

    suspend fun restoreMissingFiles() {
        dao.getLibriVoxBooks()
            .flatMap { book -> dao.getTracks(book.id) }
            .filter { track -> isLocal(track.documentUri) && !existsLocally(track.documentUri) }
            .forEach { track -> track.remoteUrl?.let { dao.updateTrackUri(track.id, it) } }
    }

    private fun existsLocally(documentUri: String) = Uri.parse(documentUri).path?.let { File(it).exists() } == true

    private fun isSafeName(name: String) = name.isNotEmpty() && name != "." && name != ".." && '/' !in name && '\\' !in name

    companion object {
        private const val DIRECTORY = "librivox"

        fun isLocal(documentUri: String) = documentUri.startsWith("file:")
    }
}
