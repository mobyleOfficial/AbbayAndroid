package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/**
 * Tells the processors the track time of the buffer being handed to them (presentation time minus the stream offset),
 * which silence trimming needs to protect chapter and track edges. Runs on the
 * playback thread, in the same call in which the sink queues the buffer into the first processor.
 */
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
