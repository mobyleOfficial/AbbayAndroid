package com.mobyle.abbay.presentation.common.mappers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.model.BookFile
import com.model.MultipleBooks
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun MediaMetadataRetriever.toBook(context: Context, id: String): BookFile {
    val title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0L"
    val imageBitmap = embeddedPicture?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }

    return BookFile(
        id = id,
        name = title ?: "",
        thumbnail = imageBitmap?.let {
            getImageUriFromBitmap(
                context,
                imageBitmap,
                id.replace(" ", "")
            ).toString()
        },
        progress = 0L,
        duration = duration.toLong()
    )
}

fun MediaMetadataRetriever.getThumbnail(context: Context, id: String): String? {
    val imageBitmap = embeddedPicture?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }

    return imageBitmap?.let {
        getImageUriFromBitmap(
            context,
            imageBitmap,
            id.replace(" ", "")
        ).toString()
    }
}

fun List<BookFile>.toMultipleBooks(): MultipleBooks? {
    val firstBook = firstOrNull()
    return firstBook?.let {
        MultipleBooks(
            it.id,
            this,
            firstBook.name,
            firstBook.thumbnail,
            0L,
            this.sumOf { it.duration })
    }
}

// todo: add images into multiple files book
fun getImageUriFromBitmap(context: Context, bitmap: Bitmap, name: String): Uri {
    val filesDir = context.filesDir
    val imageFile = File(filesDir, name)

    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        return imageFile.toUri()
    } catch (e: IOException) {
        e.printStackTrace()
        return Uri.parse("")
    } finally {
        try {
            fos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

//    val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "File", null)
//    return Uri.parse(path?.toString().orEmpty())
    }
}