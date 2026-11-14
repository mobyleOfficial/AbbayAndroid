package com.mobyle.abbay.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.repository.LegacyMigrationRepository
import com.repository.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LibraryScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val libraryRepository: LibraryRepository,
    private val legacyMigrationRepository: LegacyMigrationRepository,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        libraryRepository.fullScan()
        legacyMigrationRepository.importLegacyProgress()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "library-full-scan"
    }
}
