package com.usecase

import com.repository.BackupRepository
import javax.inject.Inject

class ExportBackup @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(destinationUri: String) = repository.export(destinationUri)
}
