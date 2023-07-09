package com.mobyle.abbay.infra.di

import android.content.Context
import androidx.room.Room
import com.mobyle.abbay.R
import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.infra.database.AbbayDatabase
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
        Room.databaseBuilder(
            context,
            AbbayDatabase::class.java,
            context.getString(R.string.app_name)
        ).build()

    @Provides
    fun providesBooksDao(database: AbbayDatabase): BooksDao =
        database.getBooksDao()
}
