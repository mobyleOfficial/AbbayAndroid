package com.mobyle.abbay.presentation.playback.policy

object PlaybackErrorPolicy {
    enum class ErrorKind {
        STORAGE,

        UNPLAYABLE,
    }

    sealed interface Decision {
        data class SkipToNext(val failedTrackIndex: Int) : Decision

        data object StopBookUnplayable : Decision

        data object StopAtEnd : Decision

        data object StopStorageUnavailable : Decision
    }

    fun decide(kind: ErrorKind, failedTrackIndex: Int, trackCount: Int, previouslyFailedTracks: Set<Int>): Decision {
        if (kind == ErrorKind.STORAGE) return Decision.StopStorageUnavailable

        val failedTracks = previouslyFailedTracks + failedTrackIndex
        return when {
            failedTracks.size >= trackCount -> Decision.StopBookUnplayable
            failedTrackIndex < trackCount - 1 -> Decision.SkipToNext(failedTrackIndex)
            else -> Decision.StopAtEnd
        }
    }
}
