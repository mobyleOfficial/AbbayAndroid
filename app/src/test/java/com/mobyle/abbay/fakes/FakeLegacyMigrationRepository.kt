package com.mobyle.abbay.fakes

import com.repository.LegacyMigrationRepository

class FakeLegacyMigrationRepository(
    var legacyFolder: String? = null,
    var isStillAccessible: Boolean = false,
) : LegacyMigrationRepository {
    var migrated = false
    var importCalls = 0

    override suspend fun legacyFolderUri(): String? = legacyFolder.takeUnless { migrated }

    override suspend fun isLegacyFolderStillAccessible(): Boolean = isStillAccessible

    override suspend fun markLegacyFolderMigrated() {
        migrated = true
    }

    override suspend fun importLegacyProgress(): Int {
        importCalls++
        return 0
    }
}
