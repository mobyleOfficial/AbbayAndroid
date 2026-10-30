package com.mobyle.abbay.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Only user presets are stored; built-in ones live in code. */
@Entity(tableName = "equalizer_preset")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "gains_json") val gainsJson: String,
    @ColumnInfo(name = "built_in") val builtIn: Boolean = false,
)
