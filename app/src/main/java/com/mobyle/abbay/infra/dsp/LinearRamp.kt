package com.mobyle.abbay.infra.dsp

class LinearRamp(initial: Float, private val rampFrames: Int) {
    var current: Float = initial
        private set
    var target: Float = initial
        private set
    private var step = 0f
    private var remainingFrames = 0

    val isRamping: Boolean get() = remainingFrames > 0

    fun moveTo(value: Float) {
        if (value == target && !isRamping) return
        target = value
        remainingFrames = rampFrames.coerceAtLeast(1)
        step = (target - current) / remainingFrames
    }

    fun snapTo(value: Float) {
        target = value
        current = value
        remainingFrames = 0
    }

    fun next(): Float {
        if (remainingFrames > 0) {
            remainingFrames--
            current = if (remainingFrames == 0) target else current + step
        }
        return current
    }

    companion object {
        fun framesFor(sampleRate: Int, milliseconds: Int = 20): Int = sampleRate * milliseconds / 1_000
    }
}
