package com.mobyle.abbay.data.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.ChapterEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.database.entity.TrackEntity

data class BookWithDetails(
    @Embedded val book: BookEntity,
    @Relation(parentColumn = "id", entityColumn = "book_id") val tracks: List<TrackEntity>,
    @Relation(parentColumn = "id", entityColumn = "book_id") val listeningState: ListeningStateEntity?,
    @Relation(parentColumn = "id", entityColumn = "book_id") val chapters: List<ChapterEntity> = emptyList(),
)
