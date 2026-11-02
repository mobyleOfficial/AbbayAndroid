package com.mobyle.abbay.infra.di

import com.mobyle.abbay.infra.readalong.VoskReadAlongTranscriber
import com.mobyle.abbay.presentation.readalong.ReadAlongDependencies
import com.readalong.ReadAlongTranscriber
import com.usecase.GetReadAlongAudioSource
import com.usecase.GetReadAlongLanguage
import com.usecase.GetReadAlongTranscript
import com.usecase.SaveReadAlongTranscript
import com.usecase.SetReadAlongLanguage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReadAlongModule {
    @Binds
    internal abstract fun bindReadAlongTranscriber(transcriber: VoskReadAlongTranscriber): ReadAlongTranscriber

    companion object {
        @Provides
        @Singleton
        @Suppress("LongParameterList")
        fun provideReadAlongDependencies(
            transcriber: ReadAlongTranscriber,
            getTranscript: GetReadAlongTranscript,
            saveTranscript: SaveReadAlongTranscript,
            getLanguage: GetReadAlongLanguage,
            setLanguage: SetReadAlongLanguage,
            audioSource: GetReadAlongAudioSource,
        ): ReadAlongDependencies = ReadAlongDependencies(
            transcriber = transcriber,
            getTranscript = getTranscript,
            saveTranscript = saveTranscript,
            getLanguage = getLanguage,
            setLanguage = setLanguage,
            audioSource = audioSource,
        )
    }
}
