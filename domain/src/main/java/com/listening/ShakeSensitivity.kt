@file:Suppress("MagicNumber")

package com.listening

enum class ShakeSensitivity(val thresholdMetersPerSecondSquared: Float) {
    LOW(18f),
    MEDIUM(14f),
    HIGH(10f),
}
