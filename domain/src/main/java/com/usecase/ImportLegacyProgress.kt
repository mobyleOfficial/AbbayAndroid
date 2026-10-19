package com.usecase

import com.repository.LegacyMigrationRepository
import javax.inject.Inject

class ImportLegacyProgress @Inject constructor(private val repository: LegacyMigrationRepository) {
    suspend operator fun invoke() = repository.importLegacyProgress()
}
