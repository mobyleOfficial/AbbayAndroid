package com.usecase

import com.repository.LegacyMigrationRepository
import javax.inject.Inject

/** The user picked the folder again on the one-time migration screen, or skipped it. */
class CompleteLegacyFolderMigration @Inject constructor(private val repository: LegacyMigrationRepository) {
    suspend operator fun invoke() = repository.markLegacyFolderMigrated()
}
