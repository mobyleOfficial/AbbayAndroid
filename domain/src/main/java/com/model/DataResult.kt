package com.model

sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}

enum class DataError {
    ACCESS_LOST,

    UNREADABLE,

    INVALID_FORMAT,

    NETWORK,

    NOT_AN_AUDIOBOOK,
}
