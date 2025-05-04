package com.mobyle.abbay.presentation.booklist

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.mobyle.abbay.presentation.common.mappers.getThumbnail
import com.mobyle.abbay.presentation.common.mappers.toMultipleBooks
import com.mobyle.abbay.presentation.utils.getAlbumTitle
import com.mobyle.abbay.presentation.utils.getDuration
import com.mobyle.abbay.presentation.utils.getFileName
import com.mobyle.abbay.presentation.utils.getId
import com.mobyle.abbay.presentation.utils.getTitle
import com.model.Book
import com.model.BookFile
import com.model.MultipleBooks
import java.io.File

fun Uri.resolveContentUri(context: Context): String? {
    val docUri = DocumentsContract.buildDocumentUriUsingTree(
        this,
        DocumentsContract.getTreeDocumentId(this)
    )
    val docCursor = context.contentResolver.query(docUri, null, null, null, null)

    var str: String = ""

    // get a string of the form : primary:Audiobooks or 1407-1105:Audiobooks
    while (docCursor!!.moveToNext()) {
        str = docCursor.getString(0)
        if (str.matches(Regex(".*:.*"))) break //Maybe useless
    }

    docCursor.close()

    val split = str.split(":")

    val base: File =
        if (split[0] == "primary") Environment.getExternalStorageDirectory()
        else File("/storage/${split[0]}")

    if (!base.isDirectory) {
        return null
    }

    return File(base, split[1]).canonicalPath
}

fun Uri.getBooks(context: Context): List<Book>? {
    return this.resolveContentUri(context)?.let { folderPath ->
        val contentResolver: ContentResolver = context.contentResolver
        val bookUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val bookCursor = contentResolver.query(bookUri, null, null, null, null)
        if (bookCursor != null && bookCursor.moveToFirst()) {
            val filesHashMap = mutableMapOf<String, List<BookFile>>()

            do {
                bookCursor.getColumnIndex(MediaStore.Audio.Media.DATA).let {
                    if (it != -1) {
                        val filePath = bookCursor.getString(it)

                        if (filePath.contains(folderPath)) {
                            val id = bookCursor.getId().orEmpty()
                            val title = bookCursor.getAlbumTitle()
                            val fileName = bookCursor.getFileName().orEmpty()
                            val progress = 0L
                            val duration = bookCursor.getDuration() ?: 0L
                            val fileFolderPath =
                                bookCursor.getString(it).substringBeforeLast("/")
                            val thumbnail = null

                            val book = BookFile(
                                id = id,
                                name = title ?: fileName,
                                fileName = fileName,
                                thumbnail = thumbnail,
                                progress = progress,
                                duration = duration,
                                speed = 1f
                            )

                            filesHashMap[fileFolderPath]?.let {
                                val newList = it.toMutableList()
                                newList.add(book)
                                filesHashMap[fileFolderPath] = newList.toList()
                            } ?: run {
                                filesHashMap[fileFolderPath] = listOf(book)
                            }
                        }
                    }
                }
            } while (bookCursor.moveToNext())
            bookCursor.close()

            val filesList = filesHashMap.mapValues {
                if (it.value.size == 1) {
                    it.value.first()
                } else {
                    it.value.toMultipleBooks()
                }
            }.values.toList().filterNotNull()

            filesList.toList()
        } else {
            null
        }
    } ?: run {
        null
    }
}

fun Book?.getThumb(context: Context): Book? {
    val metadataRetriever = MediaMetadataRetriever()
    metadataRetriever.setDataSource(
        context,
        Uri.parse("content://media/external/audio/media/${this?.id}")
    )
    val thumb = metadataRetriever.getThumbnail(context, this?.id.orEmpty())
    return when (this) {
        is MultipleBooks -> this.copy(thumbnail = thumb)
        is BookFile -> this.copy(thumbnail = thumb)
        else -> this
    }
}