package com.repository

import com.model.BackupSummary
import com.model.DataResult

/** Export and import of the whole listening state as one JSON file. */
interface BackupRepository {
    suspend fun export(destinationUri: String): DataResult<BackupSummary>

    /** Merges the file into the library: books matched by sidecar id, then by relative path; newer state wins. */
    suspend fun import(sourceUri: String): DataResult<BackupSummary>
}
