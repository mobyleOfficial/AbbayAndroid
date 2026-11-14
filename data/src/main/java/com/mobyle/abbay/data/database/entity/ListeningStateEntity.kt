package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_state",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["book_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["last_played_at"])],
)
data class ListeningStateEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "track_index") val trackIndex: Int = 0,
    @ColumnInfo(name = "position_in_track_ms") val positionInTrackMs: Long = 0L,
    @ColumnInfo(name = "book_position_ms") val bookPositionMs: Long = 0L,
    val state: String = "NEW",
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long? = null,
    @ColumnInfo(name = "last_paused_at") val lastPausedAt: Long? = null,
    val speed: Float = 1f,
    @ColumnInfo(name = "pitch_semitones") val pitchSemitones: Float = 0f,
    @ColumnInfo(name = "silence_trim_level") val silenceTrimLevel: String = "OFF",
    @ColumnInfo(name = "boost_level") val boostLevel: String = "OFF",
    val mono: Boolean = false,
    val balance: Float = 0f,
    @ColumnInfo(name = "noise_reduction_level") val noiseReductionLevel: String = "OFF",
    @ColumnInfo(name = "equalizer_json") val equalizerJson: String? = null,
    @ColumnInfo(name = "repeat_mode") val repeatMode: String = "OFF",
    @ColumnInfo(name = "loop_a_ms") val loopAMs: Long? = null,
    @ColumnInfo(name = "loop_b_ms") val loopBMs: Long? = null,
    @ColumnInfo(name = "skip_intro_s") val skipIntroSeconds: Int = 0,
    @ColumnInfo(name = "skip_outro_s") val skipOutroSeconds: Int = 0,
    @ColumnInfo(name = "sort_by_track_tag") val sortByTrackTag: Boolean = false,
    @ColumnInfo(name = "last_opened_document") val lastOpenedDocument: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
    @ColumnInfo(name = "silence_saved_ms", defaultValue = "0") val silenceSavedMs: Long = 0L,
)
