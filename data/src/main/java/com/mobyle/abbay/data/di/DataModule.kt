package com.mobyle.abbay.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mobyle.abbay.data.backup.BackupRepositoryImpl
import com.mobyle.abbay.data.converter.ConverterRepositoryImpl
import com.mobyle.abbay.data.effects.AudioEffectsRepositoryImpl
import com.mobyle.abbay.data.effects.PlaybackOptionsRepositoryImpl
import com.mobyle.abbay.data.extras.CharacterRepositoryImpl
import com.mobyle.abbay.data.extras.QueueRepositoryImpl
import com.mobyle.abbay.data.extras.StatisticsRepositoryImpl
import com.mobyle.abbay.data.repository.LegacyMigrationRepositoryImpl
import com.mobyle.abbay.data.repository.LibraryRepositoryImpl
import com.mobyle.abbay.data.repository.ListeningRepositoryImpl
import com.mobyle.abbay.data.repository.SettingsRepositoryImpl
import com.mobyle.abbay.data.settings.legacyPreferencesMigration
import com.mobyle.abbay.data.storage.DocumentTreeSource
import com.mobyle.abbay.data.storage.SafDocumentTreeDataSource
import com.mobyle.abbay.data.subtitles.SubtitleRepositoryImpl
import com.repository.AudioEffectsRepository
import com.repository.BackupRepository
import com.repository.CharacterRepository
import com.repository.ConverterRepository
import com.repository.LegacyMigrationRepository
import com.repository.LibraryRepository
import com.repository.ListeningRepository
import com.repository.PlaybackOptionsRepository
import com.repository.QueueRepository
import com.repository.SettingsRepository
import com.repository.StatisticsRepository
import com.repository.SubtitleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions") // One binding per repository.
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindLibraryRepository(implementation: LibraryRepositoryImpl): LibraryRepository

    @Binds
    abstract fun bindListeningRepository(implementation: ListeningRepositoryImpl): ListeningRepository

    @Binds
    abstract fun bindAudioEffectsRepository(implementation: AudioEffectsRepositoryImpl): AudioEffectsRepository

    @Binds
    abstract fun bindPlaybackOptionsRepository(implementation: PlaybackOptionsRepositoryImpl): PlaybackOptionsRepository

    @Binds
    abstract fun bindBackupRepository(implementation: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindQueueRepository(implementation: QueueRepositoryImpl): QueueRepository

    @Binds
    abstract fun bindStatisticsRepository(implementation: StatisticsRepositoryImpl): StatisticsRepository

    @Binds
    abstract fun bindCharacterRepository(implementation: CharacterRepositoryImpl): CharacterRepository

    @Binds
    abstract fun bindConverterRepository(implementation: ConverterRepositoryImpl): ConverterRepository

    @Binds
    abstract fun bindSubtitleRepository(implementation: SubtitleRepositoryImpl): SubtitleRepository

    @Binds
    abstract fun bindSettingsRepository(implementation: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindLegacyMigrationRepository(implementation: LegacyMigrationRepositoryImpl): LegacyMigrationRepository

    @Binds
    abstract fun bindDocumentTreeSource(implementation: SafDocumentTreeDataSource): DocumentTreeSource

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                migrations = listOf(legacyPreferencesMigration(context)),
                produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE_NAME) },
            )

        private const val SETTINGS_FILE_NAME = "settings"
    }
}
