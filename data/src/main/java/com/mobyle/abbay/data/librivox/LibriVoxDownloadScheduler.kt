package com.mobyle.abbay.data.librivox

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LibriVoxDownloadScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun enqueue(bookId: Long, allowsCellular: Boolean, replace: Boolean) {
        val network = if (allowsCellular) NetworkType.CONNECTED else NetworkType.UNMETERED
        val request = OneTimeWorkRequestBuilder<LibriVoxDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_DELAY_SECONDS, TimeUnit.SECONDS)
            .setInputData(workDataOf(LibriVoxDownloadWorker.KEY_BOOK_ID to bookId))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(bookId),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(bookId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(bookId))
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    private fun uniqueName(bookId: Long) = "librivox-download-$bookId"

    private companion object {
        const val TAG = "librivox-download"
        const val RETRY_DELAY_SECONDS = 30L
    }
}
