package com.mobyle.abbay.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mobyle.abbay.data.database.dao.AudioEffectsDao
import com.mobyle.abbay.data.database.dao.ExtrasDao
import com.mobyle.abbay.data.database.dao.LegacyDao
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.dao.ListeningDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.BookmarkEntity
import com.mobyle.abbay.data.database.entity.ChapterEntity
import com.mobyle.abbay.data.database.entity.CharacterNoteEntity
import com.mobyle.abbay.data.database.entity.EqualizerPresetEntity
import com.mobyle.abbay.data.database.entity.HistoryEventEntity
import com.mobyle.abbay.data.database.entity.LegacyBookFileEntity
import com.mobyle.abbay.data.database.entity.LegacyMultipleBooksEntity
import com.mobyle.abbay.data.database.entity.LibraryRootEntity
import com.mobyle.abbay.data.database.entity.ListeningDayEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.database.entity.QueueEntryEntity
import com.mobyle.abbay.data.database.entity.TrackEntity

@Database(
    entities = [
        LibraryRootEntity::class,
        BookEntity::class,
        TrackEntity::class,
        ChapterEntity::class,
        ListeningStateEntity::class,
        LegacyBookFileEntity::class,
        LegacyMultipleBooksEntity::class,
        BookmarkEntity::class,
        HistoryEventEntity::class,
        EqualizerPresetEntity::class,
        ListeningDayEntity::class,
        QueueEntryEntity::class,
        CharacterNoteEntity::class,
    ],
    version = 7,
)
abstract class AbbayDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    abstract fun legacyDao(): LegacyDao

    abstract fun listeningDao(): ListeningDao

    abstract fun audioEffectsDao(): AudioEffectsDao

    abstract fun extrasDao(): ExtrasDao

    companion object {
        const val DATABASE_NAME = "Abbay"
    }
}
