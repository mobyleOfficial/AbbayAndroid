package com.converter

/** Converts interleaved float PCM from one sample rate to another, as a stream. */
interface AudioResampler {
    /** Resamples [input]; returns the frames produced so far (the filter keeps a short tail until [flush]). */
    fun process(input: FloatArray): FloatArray

    /** Returns the tail kept by the filter at the end of the stream. */
    fun flush(): FloatArray
}

/** Creates [AudioResampler]s; the implementation lives with the app's audio processing. */
fun interface AudioResamplerFactory {
    /** [passbandFraction]: where the passband ends, as a fraction of the lower Nyquist frequency. */
    fun create(inputRate: Int, outputRate: Int, channels: Int, passbandFraction: Double): AudioResampler
}
