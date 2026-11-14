package com.converter

interface AudioResampler {
    fun process(input: FloatArray): FloatArray

    fun flush(): FloatArray
}

fun interface AudioResamplerFactory {
    fun create(inputRate: Int, outputRate: Int, channels: Int, passbandFraction: Double): AudioResampler
}
