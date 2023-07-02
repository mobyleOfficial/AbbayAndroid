package com.mobyle.abbay.data.di

import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSource
import com.mobyle.abbay.data.datasource.local.books.BooksLocalDataSourceImpl
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
    fun getFeedRemoteDataSource(): BooksLocalDataSource = BooksLocalDataSourceImpl()

    @Singleton
    @Provides
    fun getBooksRepository(
        localDataSource: BooksLocalDataSource
    ): BooksRepository = BooksRepositoryImpl(localDataSource)
}