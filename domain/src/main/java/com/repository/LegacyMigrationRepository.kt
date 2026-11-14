package com.repository

interface LegacyMigrationRepository {
    suspend fun legacyFolderUri(): String?

    suspend fun isLegacyFolderStillAccessible(): Boolean

    suspend fun markLegacyFolderMigrated()

    suspend fun importLegacyProgress(): Int
}
