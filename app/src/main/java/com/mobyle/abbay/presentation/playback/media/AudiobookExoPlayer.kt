package com.mobyle.abbay.presentation.playback.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
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

/**
 * The service's player: exact VBR seeking, speech attributes, noisy handling, wake
 * lock, and the project's audio effects chain.
 */
@OptIn(UnstableApi::class)
object AudiobookExoPlayer {
    /** [route] chooses where the processed audio goes: the device, or a Cast receiver. */
    fun build(context: Context, effects: EffectsState, route: AudioRoute = AudioRoute()): ExoPlayer {
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        // Speed goes to the AudioTrack (the platform's Sonic time-stretch), so it also applies to audio already
        // buffered: ~10 ms instead of the ~750 ms buffer delay of in-app processing.
        val renderersFactory = EffectsRenderersFactory(context, effects, route).setEnableAudioTrackPlaybackParams(true)

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context, extractorsFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }
}

/** Builds the audio sink with the effects chain. */
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
        // Cast phase 2: the same effects, with their own processor instances, ahead of a live stream to the receiver.
        val streaming = StreamingAudioSink(AudiobookAudioChain(effects).audioProcessors.toList(), route)
        return TrackTimeAudioSink(RoutingAudioSink(sink, streaming, route), effects)
    }
}
