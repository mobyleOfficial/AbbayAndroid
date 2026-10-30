package com.usecase

import com.model.DataResult
import com.repository.LegacyMigrationRepository
import com.repository.LibraryRepository
import javax.inject.Inject

/**
 * Storage-permission → SAF migration. When the previous version's
 * folder permission persisted, the folder becomes a root without asking. Returns the legacy folder URI when the user
 * must pick the folder again, or null when nothing is left to do.
 */
class PrepareLegacyFolderMigration @Inject constructor(
    private val legacyRepository: LegacyMigrationRepository,
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(): String? {
        val legacyFolderUri = legacyRepository.legacyFolderUri() ?: return null

        if (legacyRepository.isLegacyFolderStillAccessible() &&
            libraryRepository.addRoot(legacyFolderUri) is DataResult.Success
        ) {
            legacyRepository.markLegacyFolderMigrated()
            libraryRepository.requestFullScan()
            return null
        }

        return legacyFolderUri
    }
}
