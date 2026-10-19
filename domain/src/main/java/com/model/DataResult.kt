package com.model

/** Outcome of a data-source call. Data sources map failures to [Failure]; repositories never catch. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}

enum class DataError {
    /** The persisted folder permission was revoked or the folder no longer exists. */
    ACCESS_LOST,

    /** Any other I/O failure. */
    UNREADABLE,

    /** The file was read but is not what was expected (for example, not an Abbay backup). */
    INVALID_FORMAT,
}
