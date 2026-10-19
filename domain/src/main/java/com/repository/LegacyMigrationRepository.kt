package com.repository

/** Migration from the storage-permission version of AbbayAndroid. */
interface LegacyMigrationRepository {
    /** The tree URI the previous version saved as "books folder", if it still needs to be migrated. */
    suspend fun legacyFolderUri(): String?

    /** True when the previous version's folder can be reused without asking the user (its permission persisted). */
    suspend fun isLegacyFolderStillAccessible(): Boolean

    /** Called once the legacy folder was added again (automatically or by the user). */
    suspend fun markLegacyFolderMigrated()

    /** Imports progress and speed of the previous version's books into the scanned library. Returns imported books. */
    suspend fun importLegacyProgress(): Int
}
