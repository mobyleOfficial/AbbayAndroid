package com.player

/** A finished book hands over to the next queued one after a countdown. */
data class UpNext(val bookId: String, val title: String, val secondsLeft: Int)
