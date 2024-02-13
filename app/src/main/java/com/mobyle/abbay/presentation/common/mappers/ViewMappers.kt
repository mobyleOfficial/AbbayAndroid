package com.mobyle.abbay.presentation.common.mappers

import android.media.MediaMetadataRetriever
import com.model.BookFile
import com.model.BookFolder

fun MediaMetadataRetriever.toBook(path: String): BookFile {
    val title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0L"
    return BookFile(path, title ?: "", embeddedPicture, duration.toLong())
}

fun List<BookFile>.toFolder(): BookFolder {
    val firstBook = first()
    return BookFolder(this, firstBook.name, firstBook.thumbnail, this.sumOf { it.duration })
}