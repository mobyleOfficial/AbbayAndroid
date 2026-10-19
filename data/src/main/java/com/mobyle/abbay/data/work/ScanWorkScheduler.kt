package com.mobyle.abbay.data.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ScanWorkScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun enqueueFullScan() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            LibraryScanWorker.UNIQUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<LibraryScanWorker>().build(),
        )
    }
}
