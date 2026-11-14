package com.mobyle.abbay.presentation.playback.di

import com.mobyle.abbay.presentation.playback.PlayerControllerImpl
import com.mobyle.abbay.presentation.playback.diagnostics.DiagnosticsRepositoryImpl
import com.mobyle.abbay.presentation.playback.remote.RemotePlaybackBridge
import com.player.PlayerController
import com.repository.DiagnosticsRepository
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds
    abstract fun bindPlayerController(implementation: PlayerControllerImpl): PlayerController

    @BindsOptionalOf
    abstract fun optionalRemotePlayback(): RemotePlaybackBridge

    @Binds
    abstract fun bindDiagnosticsRepository(implementation: DiagnosticsRepositoryImpl): DiagnosticsRepository
}
