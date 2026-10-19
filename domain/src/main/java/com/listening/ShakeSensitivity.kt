@file:Suppress("MagicNumber") // The values are the table.

package com.listening

/** Shake thresholds of the sleep timer, in m/s². */
enum class ShakeSensitivity(val thresholdMetersPerSecondSquared: Float) {
    LOW(18f),
    MEDIUM(14f),
    HIGH(10f),
}
