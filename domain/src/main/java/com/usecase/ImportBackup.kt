package com.usecase

import com.repository.BackupRepository
import javax.inject.Inject

class ImportBackup @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(sourceUri: String) = repository.import(sourceUri)
}
