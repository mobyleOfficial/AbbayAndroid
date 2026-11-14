package com.usecase

import com.model.DataResult
import com.repository.LegacyMigrationRepository
import com.repository.LibraryRepository
import javax.inject.Inject

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
