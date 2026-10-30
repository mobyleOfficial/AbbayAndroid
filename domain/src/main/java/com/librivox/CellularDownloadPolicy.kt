package com.librivox

import com.model.NetworkStatus

object CellularDownloadPolicy {
    const val LARGE_DOWNLOAD_BYTES = 99_000_000L

    sealed interface Decision {
        data class Proceed(val allowsCellular: Boolean) : Decision

        data object AskUser : Decision
    }

    fun decide(totalBytes: Long, network: NetworkStatus): Decision = when {
        totalBytes <= LARGE_DOWNLOAD_BYTES -> Decision.Proceed(allowsCellular = true)
        network.isMetered -> Decision.AskUser
        else -> Decision.Proceed(allowsCellular = false)
    }
}
