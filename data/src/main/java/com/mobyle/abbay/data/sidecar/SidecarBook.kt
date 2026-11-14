package com.mobyle.abbay.data.sidecar

data class SidecarBook(
    val id: String,
    val updatedAt: Long,
    val position: Position?,
    val state: String,
    val lastPlayedAt: Long?,
    val lastPausedAt: Long?,
    val audio: Audio = Audio(),
    val repeatMode: String = "OFF",
    val loopAMs: Long? = null,
    val loopBMs: Long? = null,
    val skipIntroSeconds: Int = 0,
    val skipOutroSeconds: Int = 0,
    val sortByTrackTag: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val characters: List<Character> = emptyList(),
    val overrides: Overrides = Overrides(),
) {
    data class Position(val trackFileName: String, val positionInTrackMs: Long, val bookPositionMs: Long)

    data class Audio(
        val speed: Float = 1f,
        val pitchSemitones: Float = 0f,
        val silenceTrim: String = "OFF",
        val boost: String = "OFF",
        val mono: Boolean = false,
        val balance: Float = 0f,
        val noiseReduction: String = "OFF",
        val equalizerJson: String? = null,
    )

    data class Bookmark(val bookPositionMs: Long, val chapterTitle: String, val note: String, val createdAt: Long)

    data class Character(val name: String, val description: String, val firstPositionMs: Long?)

    data class Overrides(
        val title: String? = null,
        val author: String? = null,
        val narrator: String? = null,
        val description: String? = null,
    )
}
