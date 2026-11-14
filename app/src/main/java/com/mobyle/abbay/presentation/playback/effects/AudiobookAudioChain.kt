package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class AudiobookAudioChain(state: EffectsState) : AudioProcessorChain {
    private val silenceTrim = SilenceTrimProcessor(state)
    private val processors: Array<AudioProcessor> = arrayOf(
        NoiseReductionProcessor(state),
        silenceTrim,
        EqualizerProcessor(state),
        ChannelProcessor(state),
        LoudnessProcessor(state),
    )

    override fun getAudioProcessors(): Array<AudioProcessor> = processors

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters = playbackParameters

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean = false

    override fun getMediaDuration(playoutDuration: Long): Long = playoutDuration

    override fun getSkippedOutputFrameCount(): Long = silenceTrim.skippedFrames
}
