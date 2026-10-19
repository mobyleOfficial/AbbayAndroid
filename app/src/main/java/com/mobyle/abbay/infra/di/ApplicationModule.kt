package com.mobyle.abbay.infra.di

import android.app.Application
import android.content.Context
import com.converter.AudioResamplerFactory
import com.mobyle.abbay.infra.dsp.ResamplerFactory
import com.mobyle.abbay.infra.language.AppLanguageStore
import com.mobyle.abbay.presentation.widget.WidgetNowPlayingPublisher
import com.player.NowPlayingPublisher
import com.repository.LanguageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {
    @Binds
    internal abstract fun bindContext(application: Application): Context

    @Binds
    internal abstract fun bindLanguageRepository(store: AppLanguageStore): LanguageRepository

    /** The converter in `data` resamples through the app's DSP code. */
    @Binds
    internal abstract fun bindResamplerFactory(factory: ResamplerFactory): AudioResamplerFactory

    companion object {
        /** The home screen widgets of `:widget` receive what plays. */
        @Provides
        @Singleton
        fun provideNowPlayingPublisher(@ApplicationContext context: Context): NowPlayingPublisher = WidgetNowPlayingPublisher(context)
    }
}
