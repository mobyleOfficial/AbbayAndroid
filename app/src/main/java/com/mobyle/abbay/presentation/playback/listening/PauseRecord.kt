package com.mobyle.abbay.presentation.playback.listening

/** A pause of [bookId] at wall-clock [pausedAt], at [bookPositionMs]. */
data class PauseRecord(val bookId: String, val pausedAt: Long, val bookPositionMs: Long)
