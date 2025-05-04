package com.mobyle.abbay.presentation.booklist.widgets.models

sealed class SpeedModel(open val speed: Float, open val text: String) {
    data object Half : SpeedModel(0.5f, "0.5x")
    data object Normal : SpeedModel(1f, "1x")
    data object OnePointTwoFive : SpeedModel(1.25f, "1.25x")
    data object OnePointFive : SpeedModel(1.5f, "1.5x")
    data object Double : SpeedModel(2f, "2x")
}