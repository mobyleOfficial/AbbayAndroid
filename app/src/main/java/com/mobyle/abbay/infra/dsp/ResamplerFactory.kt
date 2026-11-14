package com.mobyle.abbay.infra.dsp

import com.converter.AudioResampler
import com.converter.AudioResamplerFactory
import javax.inject.Inject

class ResamplerFactory @Inject constructor() : AudioResamplerFactory {
    override fun create(inputRate: Int, outputRate: Int, channels: Int, passbandFraction: Double): AudioResampler {
        val resampler = Resampler(inputRate, outputRate, channels, passbandFraction)
        return object : AudioResampler {
            override fun process(input: FloatArray): FloatArray = resampler.process(input)

            override fun flush(): FloatArray = resampler.flush()
        }
    }
}
