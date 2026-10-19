package com.mobyle.abbay.infra.dsp.nativelib

import java.io.Closeable

/**
 * One RNNoise state: denoises mono 48 kHz audio in frames of [FRAME_SIZE] samples, delayed by one frame.
 * Samples are floats in ±1; RNNoise's 16-bit scale is applied here. Not thread-safe: one instance per channel.
 */
class RnnNoise : Closeable {
    private var handle: Long = nativeCreate()
    private val scaledInput = FloatArray(FRAME_SIZE)
    private val scaledOutput = FloatArray(FRAME_SIZE)

    /** Denoises [input] into [output] (both [FRAME_SIZE] long); returns the voice probability 0…1. */
    fun process(input: FloatArray, output: FloatArray): Float {
        for (index in 0 until FRAME_SIZE) scaledInput[index] = input[index] * SHORT_SCALE
        val probability = nativeProcess(handle, scaledInput, scaledOutput)
        for (index in 0 until FRAME_SIZE) output[index] = scaledOutput[index] / SHORT_SCALE
        return probability
    }

    override fun close() {
        nativeDestroy(handle)
        handle = 0
    }

    companion object {
        const val FRAME_SIZE = 480
        const val SAMPLE_RATE = 48_000
        private const val SHORT_SCALE = 32_768f

        /** False when the native library could not be loaded (the effect is then unavailable). */
        val isAvailable: Boolean = runCatching { System.loadLibrary("abbay_rnnoise") }.isSuccess

        @JvmStatic private external fun nativeCreate(): Long

        @JvmStatic private external fun nativeDestroy(handle: Long)

        @JvmStatic private external fun nativeFrameSize(): Int

        @JvmStatic private external fun nativeProcess(handle: Long, input: FloatArray, output: FloatArray): Float

        /** The frame size compiled into the library; must equal [FRAME_SIZE]. */
        fun nativeFrameSizeOrZero(): Int = if (isAvailable) nativeFrameSize() else 0
    }
}
