package com.mobyle.abbay.presentation.playback.remote

import android.media.AudioDeviceInfo
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioSink
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class RoutingAudioSink(private val local: AudioSink, private val remote: AudioSink, private val route: AudioRoute) : AudioSink {
    private var active: AudioSink = local
    private var configuration: Configuration? = null
    private var isPlaying = false

    private val both get() = listOf(local, remote)

    override fun setListener(listener: AudioSink.Listener) = both.forEach { it.setListener(listener) }

    override fun setPlayerId(playerId: PlayerId?) = both.forEach { it.setPlayerId(playerId) }

    override fun setClock(clock: Clock) = both.forEach { it.setClock(clock) }

    override fun supportsFormat(format: Format): Boolean = local.supportsFormat(format)

    override fun getFormatSupport(format: Format): Int = local.getFormatSupport(format)

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long = active.getCurrentPositionUs(sourceEnded)

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        configuration = Configuration(inputFormat, specifiedBufferSize, outputChannels)
        active.configure(inputFormat, specifiedBufferSize, outputChannels)
    }

    override fun play() {
        isPlaying = true
        active.play()
    }

    override fun handleDiscontinuity() = active.handleDiscontinuity()

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean =
        active.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)

    override fun playToEndOfStream() = active.playToEndOfStream()

    override fun isEnded(): Boolean = active.isEnded()

    override fun hasPendingData(): Boolean = active.hasPendingData()

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) = both.forEach {
        it.setPlaybackParameters(playbackParameters)
    }

    override fun getPlaybackParameters(): PlaybackParameters = active.playbackParameters

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) = both.forEach { it.setSkipSilenceEnabled(skipSilenceEnabled) }

    override fun getSkipSilenceEnabled(): Boolean = active.skipSilenceEnabled

    override fun setAudioAttributes(audioAttributes: AudioAttributes) = both.forEach { it.setAudioAttributes(audioAttributes) }

    override fun getAudioAttributes(): AudioAttributes? = active.audioAttributes

    override fun setAudioSessionId(audioSessionId: Int) = both.forEach { it.setAudioSessionId(audioSessionId) }

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) = both.forEach { it.setAuxEffectInfo(auxEffectInfo) }

    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) = both.forEach { it.setPreferredDevice(audioDeviceInfo) }

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) = both.forEach { it.setOutputStreamOffsetUs(outputStreamOffsetUs) }

    override fun enableTunnelingV21() = local.enableTunnelingV21()

    override fun disableTunneling() = both.forEach { it.disableTunneling() }

    override fun setVolume(volume: Float) = both.forEach { it.setVolume(volume) }

    override fun pause() {
        isPlaying = false
        active.pause()
    }

    override fun flush() {
        val wanted = if (route.stream != null) remote else local
        if (wanted !== active) {
            active.pause()
            active.flush()
            active = wanted
            configuration?.let { active.configure(it.format, it.specifiedBufferSize, it.outputChannels) }
        }
        active.flush()
        if (isPlaying) active.play()
    }

    override fun reset() = both.forEach { it.reset() }

    override fun release() = both.forEach { it.release() }

    private class Configuration(val format: Format, val specifiedBufferSize: Int, val outputChannels: IntArray?)
}
