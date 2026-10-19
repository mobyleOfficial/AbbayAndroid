package com.mobyle.abbay.presentation.playback.sleep

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Short tones of the sleep timer, synthesized here so no sound asset is
 * needed: a falling two-note chime when the fade starts and a rising pair of beeps when a shake extends the timer.
 */
class TonePlayer(private val scope: CoroutineScope) {
    fun playWarningChime(volume: Float) = play(listOf(Note(CHIME_HIGH_HZ, CHIME_NOTE_MS), Note(CHIME_LOW_HZ, CHIME_NOTE_MS)), volume)

    fun playConfirmation(volume: Float) = play(listOf(Note(BEEP_LOW_HZ, BEEP_NOTE_MS), Note(BEEP_HIGH_HZ, BEEP_NOTE_MS)), volume)

    private fun play(notes: List<Note>, volume: Float) {
        scope.launch(Dispatchers.Default) {
            val samples = synthesize(notes)
            val track = runCatching { buildTrack(samples.size) }.getOrNull() ?: return@launch
            try {
                track.write(samples, 0, samples.size)
                track.setVolume(volume.coerceIn(0f, 1f))
                track.play()
                delay(notes.sumOf { it.durationMs } + RELEASE_MARGIN_MS)
            } finally {
                track.release()
            }
        }
    }

    private fun buildTrack(sampleCount: Int): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(sampleCount * BYTES_PER_SAMPLE)
        .build()

    /** Sine notes with short linear attack and release, so they do not click. */
    private fun synthesize(notes: List<Note>): ShortArray {
        val output = ArrayList<Short>()
        notes.forEach { note ->
            val length = SAMPLE_RATE * note.durationMs.toInt() / MILLISECONDS_PER_SECOND
            val ramp = SAMPLE_RATE * RAMP_MS / MILLISECONDS_PER_SECOND
            for (index in 0 until length) {
                val envelope = min(1f, min(index, length - 1 - index).toFloat() / ramp)
                val value = sin(2 * PI * note.frequencyHz * index / SAMPLE_RATE) * envelope * AMPLITUDE
                output += (value * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return output.toShortArray()
    }

    private data class Note(val frequencyHz: Double, val durationMs: Long)

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val BYTES_PER_SAMPLE = 2
        const val MILLISECONDS_PER_SECOND = 1_000
        const val RAMP_MS = 10
        const val AMPLITUDE = 0.6
        const val RELEASE_MARGIN_MS = 100L
        const val CHIME_HIGH_HZ = 880.0
        const val CHIME_LOW_HZ = 660.0
        const val CHIME_NOTE_MS = 220L
        const val BEEP_LOW_HZ = 660.0
        const val BEEP_HIGH_HZ = 990.0
        const val BEEP_NOTE_MS = 90L
    }
}
