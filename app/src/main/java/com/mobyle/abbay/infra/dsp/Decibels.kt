package com.mobyle.abbay.infra.dsp

import kotlin.math.log10
import kotlin.math.pow

/** Conversions between decibels and linear gain. */
object Decibels {
    fun toGain(decibels: Float): Float = 10f.pow(decibels / 20f)

    fun fromGain(gain: Float): Float = if (gain <= 0f) Float.NEGATIVE_INFINITY else 20f * log10(gain)
}
