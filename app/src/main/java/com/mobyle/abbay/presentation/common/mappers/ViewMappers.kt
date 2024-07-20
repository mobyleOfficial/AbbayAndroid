package com.mobyle.abbay.presentation.common.mappers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.model.BookFile
import com.model.MultipleBooks
import java.io.ByteArrayOutputStream

fun MediaMetadataRetriever.toBook(context: Context, id: String): BookFile {
    val title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0L"
    val imageBitmap = embeddedPicture?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }

    return BookFile(
        id = id,
        name = title ?: "",
        thumbnail = imageBitmap?.let { getImageUriFromBitmap(context, imageBitmap).toString() },
        progress = 0L,
        duration = duration.toLong()
    )
}

fun List<BookFile>.toMultipleBooks(): MultipleBooks? {
    val firstBook = firstOrNull()
    return firstBook?.let {
        MultipleBooks(
            "",
            this,
            firstBook.name,
            firstBook.thumbnail,
            0L,
            this.sumOf { it.duration })
    }
}

fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "File", null)
    return Uri.parse(path?.toString().orEmpty())
}