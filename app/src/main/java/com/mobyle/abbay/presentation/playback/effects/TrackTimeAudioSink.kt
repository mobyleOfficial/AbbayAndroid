package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class TrackTimeAudioSink(sink: AudioSink, private val state: EffectsState) : ForwardingAudioSink(sink) {
    private var streamOffsetUs = 0L

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        streamOffsetUs = outputStreamOffsetUs
        super.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        state.currentBufferTrackTimeUs = (presentationTimeUs - streamOffsetUs).coerceAtLeast(0L)
        return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }
}
