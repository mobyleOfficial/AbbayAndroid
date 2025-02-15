package com.mobyle.abbay.data.di

import android.content.Context
import android.content.SharedPreferences
import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSourceImpl
import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStore
import com.mobyle.abbay.data.repository.BooksRepositoryImpl
import com.mobyle.abbay.data.repository.SettingsRepositoryImpl
import com.repository.BooksRepository
import com.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
    @Provides
    fun provideBooksLocalDataSource(booksDao: BooksDao): BooksLocalDataSource =
        BooksLocalDataSourceImpl(booksDao)

    @Singleton
    @Provides
    fun provideBooksRepository(
        localDataSource: BooksLocalDataSource
    ): BooksRepository = BooksRepositoryImpl(localDataSource)

    @Singleton
    @Provides
    fun provideSettingsRepository(
        keyValueStore: KeyValueStore
    ): SettingsRepository = SettingsRepositoryImpl(keyValueStore)

    @Singleton
    @Provides
    fun provideKeyValueStore(
        sharedPreferences: SharedPreferences
    ): KeyValueStore = KeyValueStore(sharedPreferences)

    @Provides
    fun provideSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(KeyValueStore.KEY, Context.MODE_PRIVATE)
    }
}