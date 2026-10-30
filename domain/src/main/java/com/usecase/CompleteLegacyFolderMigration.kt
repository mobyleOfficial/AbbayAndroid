package com.usecase

import com.repository.LegacyMigrationRepository
import javax.inject.Inject

class CompleteLegacyFolderMigration @Inject constructor(private val repository: LegacyMigrationRepository) {
    suspend operator fun invoke() = repository.markLegacyFolderMigrated()
}
