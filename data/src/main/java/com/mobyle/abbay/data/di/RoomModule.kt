package com.mobyle.abbay.data.di

import android.content.Context
import androidx.room.Room
import com.mobyle.abbay.data.database.AbbayDatabase
import com.mobyle.abbay.data.database.Migration1To2
import com.mobyle.abbay.data.database.Migration2To3
import com.mobyle.abbay.data.database.Migration3To4
import com.mobyle.abbay.data.database.Migration4To5
import com.mobyle.abbay.data.database.Migration5To6
import com.mobyle.abbay.data.database.dao.AudioEffectsDao
import com.mobyle.abbay.data.database.dao.ExtrasDao
import com.mobyle.abbay.data.database.dao.LegacyDao
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.dao.ListeningDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RoomModule {
    @Provides
    @Singleton
    fun providesAppDatabase(@ApplicationContext context: Context): AbbayDatabase =
        Room.databaseBuilder(context, AbbayDatabase::class.java, AbbayDatabase.DATABASE_NAME)
            .addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6)
            .build()

    @Provides
    fun providesLibraryDao(database: AbbayDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun providesLegacyDao(database: AbbayDatabase): LegacyDao = database.legacyDao()

    @Provides
    fun providesListeningDao(database: AbbayDatabase): ListeningDao = database.listeningDao()

    @Provides
    fun providesAudioEffectsDao(database: AbbayDatabase): AudioEffectsDao = database.audioEffectsDao()

    @Provides
    fun providesExtrasDao(database: AbbayDatabase): ExtrasDao = database.extrasDao()
}
