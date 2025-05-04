package com.mobyle.abbay.presentation.booklist.widgets.models

sealed class BookSpeed(open val speed: Float, open val text: String) {
    data object Half : BookSpeed(0.5f, "0.5x")
    data object Normal : BookSpeed(1f, "1x")
    data object OnePointTwoFive : BookSpeed(1.25f, "1.25x")
    data object OnePointFive : BookSpeed(1.5f, "1.5x")
    data object Double : BookSpeed(2f, "2x")
}