package com.mobyle.abbay.presentation.playback.remote

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessingPipeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioSink
import com.google.common.collect.ImmutableList
import java.nio.ByteBuffer

/**
 * The audio sink of Cast phase 2: runs the decoded PCM through the same effects as local playback (its own
 * processor instances over the shared settings), then speed and pitch (Sonic, which the AudioTrack does locally), and
 * writes 16-bit PCM into the [ProcessedAudioStream] of [route]. The media position is where the **receiver** is:
 * each queued buffer leaves a checkpoint (output frame → presentation time), and the receiver's played frames are
 * mapped back through them.
 */
@OptIn(UnstableApi::class)
class StreamingAudioSink(effectProcessors: List<AudioProcessor>, private val route: AudioRoute) : AudioSink {
    private val sonic = SonicAudioProcessor()
    private val pipeline = AudioProcessingPipeline(ImmutableList.copyOf(effectProcessors + ToInt16PcmAudioProcessor() + sonic))
    private val checkpoints = ArrayDeque<Checkpoint>()
    private var listener: AudioSink.Listener? = null
    private var audioAttributes = AudioAttributes.DEFAULT
    private var playbackParameters = PlaybackParameters.DEFAULT
    private var skipSilence = false
    private var inputFormat: AudioProcessor.AudioFormat? = null
    private var outputFormat: AudioProcessor.AudioFormat? = null
    private var stream: ProcessedAudioStream? = null
    private var currentInput: ByteBuffer? = null
    private var pendingOutput: ByteBuffer? = null
    private var framesWritten = 0L
    private var isEndOfStreamQueued = false
    private var isPlaying = false

    override fun setListener(listener: AudioSink.Listener) {
        this.listener = listener
    }

    override fun supportsFormat(format: Format): Boolean = getFormatSupport(format) != AudioSink.SINK_FORMAT_UNSUPPORTED

    override fun getFormatSupport(format: Format): Int =
        if (MimeTypes.AUDIO_RAW == format.sampleMimeType && Util.isEncodingLinearPcm(format.pcmEncoding)) {
            AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY
        } else {
            AudioSink.SINK_FORMAT_UNSUPPORTED
        }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        val rate = outputFormat?.sampleRate ?: return AudioSink.CURRENT_POSITION_NOT_SET
        val played = (stream?.playedFrames() ?: return AudioSink.CURRENT_POSITION_NOT_SET).coerceAtMost(framesWritten)
        while (checkpoints.size > 1 && checkpoints[1].frame <= played) checkpoints.removeFirst()
        val checkpoint = checkpoints.firstOrNull()?.takeIf { it.frame <= played } ?: return AudioSink.CURRENT_POSITION_NOT_SET
        val outputUs = (played - checkpoint.frame) * MICROS_PER_SECOND / rate
        return checkpoint.presentationTimeUs + (outputUs * checkpoint.speed).toLong()
    }

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        val format = AudioProcessor.AudioFormat(inputFormat.sampleRate, inputFormat.channelCount, inputFormat.pcmEncoding)
        this.inputFormat = format
        applyPlaybackParameters()
        outputFormat = try {
            pipeline.configure(format)
        } catch (exception: AudioProcessor.UnhandledAudioFormatException) {
            throw AudioSink.ConfigurationException(exception, inputFormat)
        }
        flush()
    }

    override fun play() {
        isPlaying = true
        stream?.play()
    }

    override fun handleDiscontinuity() = Unit

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        val target = stream ?: return false
        if (!writePendingOutput(target)) return false
        if (currentInput !== buffer) {
            currentInput = buffer
            checkpoints.addLast(Checkpoint(framesWritten, presentationTimeUs, playbackParameters.speed))
        }
        if (pipeline.isOperational) {
            pipeline.queueInput(buffer)
            pendingOutput = pipeline.output
        } else {
            pendingOutput = buffer
        }
        writePendingOutput(target)
        val isConsumed = !buffer.hasRemaining()
        if (isConsumed) currentInput = null
        return isConsumed
    }

    override fun playToEndOfStream() {
        val target = stream ?: return
        if (!isEndOfStreamQueued && pipeline.isOperational) pipeline.queueEndOfStream()
        isEndOfStreamQueued = true
        if (pipeline.isOperational && writePendingOutput(target)) {
            pendingOutput = pipeline.output
            writePendingOutput(target)
        }
    }

    override fun isEnded(): Boolean = isEndOfStreamQueued && !hasPendingData()

    override fun hasPendingData(): Boolean {
        val target = stream ?: return false
        if (pendingOutput?.hasRemaining() == true || target.bufferedBytes > 0) return true
        val played = target.playedFrames() ?: return framesWritten > 0
        return played < framesWritten
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        // Applied at the next flush: the playback service seeks to the current position when the speed changes
        // while casting, so the receiver does not play seconds of the old speed from its buffer.
        this.playbackParameters = playbackParameters
    }

    override fun getPlaybackParameters(): PlaybackParameters = playbackParameters

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        skipSilence = skipSilenceEnabled
    }

    override fun getSkipSilenceEnabled(): Boolean = skipSilence

    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        this.audioAttributes = audioAttributes
    }

    override fun getAudioAttributes(): AudioAttributes? = audioAttributes

    override fun setAudioSessionId(audioSessionId: Int) = Unit

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) = Unit

    override fun enableTunnelingV21() = Unit

    override fun disableTunneling() = Unit

    /** The receiver has its own volume (the Cast route). */
    override fun setVolume(volume: Float) = Unit

    override fun pause() {
        isPlaying = false
        stream?.pause()
    }

    /** A seek or a new start: the receiver drops its buffer and loads a new generation. */
    override fun flush() {
        applyPlaybackParameters()
        pipeline.flush()
        checkpoints.clear()
        currentInput = null
        pendingOutput = null
        framesWritten = 0L
        isEndOfStreamQueued = false
        stream = route.stream
        val format = outputFormat ?: return
        stream?.beginGeneration(format.sampleRate, format.channelCount)
        if (isPlaying) stream?.play()
    }

    override fun reset() {
        flush()
        pipeline.reset()
        inputFormat = null
        outputFormat = null
    }

    private fun applyPlaybackParameters() {
        sonic.setSpeed(playbackParameters.speed)
        sonic.setPitch(playbackParameters.pitch)
    }

    /** Writes what the pipeline has produced; true when nothing is left waiting. */
    private fun writePendingOutput(target: ProcessedAudioStream): Boolean {
        val frameBytes = (outputFormat?.channelCount ?: return true) * ProcessedAudioStream.BYTES_PER_SAMPLE
        while (true) {
            val output = pendingOutput?.takeIf { it.hasRemaining() } ?: run {
                pendingOutput = if (pipeline.isOperational) pipeline.output.takeIf { it.hasRemaining() } else null
                pendingOutput
            } ?: return true
            val written = target.write(output)
            framesWritten += written / frameBytes
            if (output.hasRemaining()) return false
        }
    }

    private class Checkpoint(val frame: Long, val presentationTimeUs: Long, val speed: Float)

    private companion object {
        const val MICROS_PER_SECOND = 1_000_000L
    }
}

/** Where the service's audio goes: the device ([stream] null) or a Cast receiver through [stream]. */
class AudioRoute {
    @Volatile
    var stream: ProcessedAudioStream? = null
}

/** Where to seek to make the player flush its audio sink without an audible jump. */
object StreamRestart {
    /** One millisecond after [positionMs] (one before it at the very end): ExoPlayer skips a seek to the same millisecond. */
    fun positionAfter(positionMs: Long, durationMs: Long): Long =
        if (durationMs > 0 && positionMs + 1 >= durationMs) (positionMs - 1).coerceAtLeast(0L) else positionMs + 1
}
