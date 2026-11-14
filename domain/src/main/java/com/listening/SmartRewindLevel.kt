@file:Suppress("MagicNumber")

package com.listening

enum class SmartRewindLevel(val baseSeconds: Double, val growth: Double, val maximumSeconds: Double) {
    OFF(0.0, 0.0, 0.0),
    LIGHT(1.0, 2.0, 15.0),
    STANDARD(2.0, 4.0, 30.0),
    GENEROUS(3.0, 6.0, 60.0),
}
