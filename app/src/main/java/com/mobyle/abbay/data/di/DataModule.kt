package com.mobyle.abbay.data.di

import android.content.Context
import android.content.SharedPreferences
import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSourceImpl
import com.mobyle.abbay.data.datasource.local.daos.BooksDao
import com.mobyle.abbay.data.repository.BooksRepositoryImpl
import com.repository.BooksRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
    @Singleton
    @Provides
    fun getBooksLocalDataSource(booksDao: BooksDao): BooksLocalDataSource = BooksLocalDataSourceImpl(booksDao)

    @Singleton
    @Provides
    fun getBooksRepository(
        localDataSource: BooksLocalDataSource
    ): BooksRepository = BooksRepositoryImpl(localDataSource)

    @Provides
    fun provideSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
    }
}