package com.player

sealed interface PlayerError {
    data class TrackSkipped(val fileName: String) : PlayerError

    data object BookUnplayable : PlayerError

    data object StorageUnavailable : PlayerError

    data object NoiseReductionTooSlow : PlayerError

    data object NetworkUnavailable : PlayerError
}
