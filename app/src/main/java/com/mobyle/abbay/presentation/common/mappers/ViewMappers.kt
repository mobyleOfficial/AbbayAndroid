package com.mobyle.abbay.presentation.common.mappers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.model.BookFile
import com.model.MultipleBooks
import java.io.ByteArrayOutputStream

fun MediaMetadataRetriever.toBook(id: String): BookFile {
    val title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0L"

    return BookFile(
        id = id,
        name = title ?: "",
        thumbnail = embeddedPicture?.resizeImage(),
        progress = 0L,
        duration = duration.toLong()
    )
}

fun List<BookFile>.toMultipleBooks(): MultipleBooks? {
    val firstBook = firstOrNull()
    return firstBook?.let {
        MultipleBooks("", this, firstBook.name, firstBook.thumbnail?.resizeImage(), 0L, this.sumOf { it.duration })
    }

}

fun ByteArray.resizeImage(): ByteArray {
    var imagem_img = this
    while (imagem_img.size > 500000) {
        val bitmap = BitmapFactory.decodeByteArray(imagem_img, 0, imagem_img.size)
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 0.8).toInt(),
            (bitmap.height * 0.8).toInt(),
            true
        )
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.PNG, 100, stream)
        imagem_img = stream.toByteArray()
    }
    return imagem_img
}