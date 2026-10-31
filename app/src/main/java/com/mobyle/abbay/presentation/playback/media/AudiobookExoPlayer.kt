package com.mobyle.abbay.presentation.playback.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import com.mobyle.abbay.presentation.playback.effects.AudiobookAudioChain
import com.mobyle.abbay.presentation.playback.effects.EffectsState
import com.mobyle.abbay.presentation.playback.effects.TrackTimeAudioSink
import com.mobyle.abbay.presentation.playback.remote.AudioRoute
import com.mobyle.abbay.presentation.playback.remote.RoutingAudioSink
import com.mobyle.abbay.presentation.playback.remote.StreamingAudioSink
import com.repository.RemoteMediaAuth

@OptIn(UnstableApi::class)
object AudiobookExoPlayer {
    fun build(
        context: Context,
        effects: EffectsState,
        route: AudioRoute = AudioRoute(),
        remoteMediaAuth: RemoteMediaAuth = RemoteMediaAuth { emptyMap() },
    ): ExoPlayer {
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val renderersFactory = EffectsRenderersFactory(context, effects, route).setEnableAudioTrackPlaybackParams(true)

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(authorizedDataSource(context, remoteMediaAuth), extractorsFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    /** Streams from a signed-in server (Audiobookshelf) carry its token; files and other urls are left as they are. */
    private fun authorizedDataSource(context: Context, auth: RemoteMediaAuth): DataSource.Factory =
        ResolvingDataSource.Factory(DefaultDataSource.Factory(context)) { dataSpec ->
            val scheme = dataSpec.uri.scheme
            val headers = if (scheme == "http" || scheme == "https") auth.headersFor(dataSpec.uri.toString()) else emptyMap()
            if (headers.isEmpty()) dataSpec else dataSpec.withAdditionalHeaders(headers)
        }
}

@OptIn(UnstableApi::class)
private class EffectsRenderersFactory(
    context: Context,
    private val effects: EffectsState,
    private val route: AudioRoute,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink {
        val sink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(AudiobookAudioChain(effects))
            .build()
        val streaming = StreamingAudioSink(AudiobookAudioChain(effects).audioProcessors.toList(), route)
        return TrackTimeAudioSink(RoutingAudioSink(sink, streaming, route), effects)
    }
}
