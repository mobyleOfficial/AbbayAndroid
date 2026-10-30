package com.repository

import com.model.BackupSummary
import com.model.DataResult

interface BackupRepository {
    suspend fun export(destinationUri: String): DataResult<BackupSummary>

    suspend fun import(sourceUri: String): DataResult<BackupSummary>
}
