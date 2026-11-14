package com.mobyle.abbay.presentation.cast.di

import com.mobyle.abbay.presentation.cast.CastBridge
import com.mobyle.abbay.presentation.playback.remote.RemotePlaybackBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CastModule {
    @Binds
    abstract fun bindRemotePlayback(implementation: CastBridge): RemotePlaybackBridge
}
