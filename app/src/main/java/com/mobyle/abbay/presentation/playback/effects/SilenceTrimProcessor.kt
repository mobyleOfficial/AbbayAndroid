package com.mobyle.abbay.presentation.playback.effects

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.effects.SilenceTrimLevel
import com.mobyle.abbay.infra.dsp.SilenceTrimmer
import java.nio.ByteBuffer
import kotlin.math.abs

@OptIn(UnstableApi::class)
class SilenceTrimProcessor(private val state: EffectsState) : FloatPcmProcessor() {
    private var trimmer: SilenceTrimmer? = null
    private var level = SilenceTrimLevel.OFF
    private var output = FloatArray(0)
    private var savedAtLastCheck = 0L

    @Volatile
    var skippedFrames = 0L
        private set

    override fun onFormat() {
        trimmer = null
        level = SilenceTrimLevel.OFF
    }

    override fun isBypassed(): Boolean = level == SilenceTrimLevel.OFF && state.current.silenceTrim == SilenceTrimLevel.OFF

    override fun process(samples: FloatArray, frameCount: Int) = Unit

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val startFrame = state.currentBufferTrackTimeUs * sampleRate / MICROSECONDS_PER_SECOND
        val drained = followSettings(startFrame)
        if (trimmer == null && drained == 0) {
            super.queueInput(inputBuffer)
            return
        }
        val sampleCount = read(inputBuffer)
        val frames = sampleCount / channelCount
        val activeTrimmer = trimmer
        var written = drained
        if (activeTrimmer != null) {
            resyncIfMoved(activeTrimmer, startFrame)
            ensureOutput(drained + activeTrimmer.maximumOutputFrames(frames))
            written += activeTrimmer.process(samples, frames, output, destinationOffset = drained)
            countSaved(activeTrimmer)
        } else {
            ensureOutput(drained + frames)
            samples.copyInto(output, drained * channelCount, 0, sampleCount)
            written += frames
        }
        val buffer = replaceOutputBuffer(bytesFor(written * channelCount))
        write(output, written * channelCount, buffer)
        buffer.flip()
    }

    override fun onQueueEndOfStream() {
        val activeTrimmer = trimmer ?: return
        ensureOutput(activeTrimmer.maximumOutputFrames(0))
        val written = activeTrimmer.drain(output)
        if (written > 0) {
            val buffer = replaceOutputBuffer(bytesFor(written * channelCount))
            write(output, written * channelCount, buffer)
            buffer.flip()
        }
    }

    override fun onFlush() {
        trimmer?.reset(state.currentBufferTrackTimeUs * sampleRate / MICROSECONDS_PER_SECOND)
        trimmer?.let { savedAtLastCheck = it.savedFrames }
    }

    private fun followSettings(startFrame: Long): Int {
        val wanted = state.current.silenceTrim
        if (wanted == level) return 0
        var drained = 0
        trimmer?.let { old ->
            ensureOutput(old.maximumOutputFrames(0))
            drained = old.drain(output)
            countSaved(old)
        }
        level = wanted
        trimmer = EffectsState.trimSettingsOf(wanted)?.let { settings ->
            SilenceTrimmer(channelCount, sampleRate, settings) { start, end ->
                state.trackBoundaries.isProtected(start * MICROSECONDS_PER_SECOND / sampleRate, end * MICROSECONDS_PER_SECOND / sampleRate)
            }.also { it.reset(startFrame) }
        }
        savedAtLastCheck = 0L
        return drained
    }

    private fun resyncIfMoved(activeTrimmer: SilenceTrimmer, startFrame: Long) {
        if (abs(activeTrimmer.nextInputFrame - startFrame) > sampleRate / RESYNC_FRACTION) activeTrimmer.rebase(startFrame)
    }

    private fun countSaved(activeTrimmer: SilenceTrimmer) {
        val newlySaved = activeTrimmer.savedFrames - savedAtLastCheck
        if (newlySaved <= 0) return
        savedAtLastCheck = activeTrimmer.savedFrames
        skippedFrames += newlySaved
        state.addSavedSilence(newlySaved * MICROSECONDS_PER_SECOND / sampleRate)
    }

    private fun ensureOutput(frames: Int) {
        if (output.size < frames * channelCount) output = output.copyOf(frames * channelCount)
    }

    override fun onReset() {
        trimmer = null
        level = SilenceTrimLevel.OFF
    }

    private companion object {
        const val MICROSECONDS_PER_SECOND = 1_000_000L

        const val RESYNC_FRACTION = 10
    }
}
