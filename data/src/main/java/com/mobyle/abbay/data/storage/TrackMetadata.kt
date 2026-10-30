package com.mobyle.abbay.data.storage

data class TrackMetadata(
    val durationMs: Long,
    val hasAudio: Boolean,
    val album: String?,
    val albumArtist: String?,
    val artist: String?,
    val composer: String?,
    val title: String?,
    val trackNumber: Int?,
)
