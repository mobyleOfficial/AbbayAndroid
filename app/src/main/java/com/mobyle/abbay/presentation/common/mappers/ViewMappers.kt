package com.mobyle.abbay.presentation.common.mappers

import android.media.MediaMetadataRetriever
import com.model.BookFile
import com.model.BookFolder

fun MediaMetadataRetriever.toBook(id: String): BookFile {
    val title = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0L"

    return BookFile(
        id = id,
        name = title ?: "",
        thumbnail = embeddedPicture,
        progress = 0L,
        duration = duration.toLong()
    )
}

fun List<BookFile>.toFolder(): BookFolder {
    val firstBook = first()
    return BookFolder(this, firstBook.name, firstBook.thumbnail, 0L, this.sumOf { it.duration })
}