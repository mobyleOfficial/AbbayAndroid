package com.readalong

/**
 * The listener's correction when the text and the audio drift apart (a seek in a variable-bitrate file, a file whose
 * timing the decoder reads differently from the player). An offset in ms moves the text against the audio: positive
 * shows it ahead (the text was late), negative behind. It applies to what is shown and to where a tapped line seeks;
 * transcription keeps following the audio itself.
 */
object ReadAlongSync {
    /** One tap of the earlier/later buttons. */
    const val STEP_MS = 500L

    /** Beyond a minute the text is wrong, not late: the language or the file is. */
    const val LIMIT_MS = 60_000L

    fun clamped(offsetMs: Long): Long = offsetMs.coerceIn(-LIMIT_MS, LIMIT_MS)

    /** The moment of the text shown while the audio is at [audioMs]. */
    fun textTime(audioMs: Long, offsetMs: Long): Long = (audioMs + offsetMs).coerceAtLeast(0)

    /** Where the audio is when the text is at [textMs]: where a tap on a line starting there seeks. */
    fun audioTime(textMs: Long, offsetMs: Long): Long = (textMs - offsetMs).coerceAtLeast(0)

    /** [offsetMs] moved by [steps] taps. */
    fun nudged(offsetMs: Long, steps: Int): Long = clamped(offsetMs + steps * STEP_MS)

    /** The offset that makes [line] start now, the audio being at [audioMs]: the listener said it is being read. */
    fun aligning(line: ReadAlongLine, audioMs: Long): Long = clamped(line.startMs - audioMs)
}
