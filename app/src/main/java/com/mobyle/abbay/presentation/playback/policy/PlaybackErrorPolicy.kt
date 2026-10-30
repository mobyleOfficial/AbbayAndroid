package com.mobyle.abbay.presentation.playback.policy

/** What the service does after a playback error. Pure logic. */
object PlaybackErrorPolicy {
    enum class ErrorKind {
        /** The file is gone or the folder permission was revoked. */
        STORAGE,

        /** The file exists but cannot be read or decoded. */
        UNPLAYABLE,
    }

    sealed interface Decision {
        /** Skip the failing track and continue with the next one. */
        data class SkipToNext(val failedTrackIndex: Int) : Decision

        /** Stop: every track of the book failed. */
        data object StopBookUnplayable : Decision

        /** Stop: the last track failed after other tracks played. */
        data object StopAtEnd : Decision

        /** Stop and let the library check its folders (reconnect banner or missing book). */
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
