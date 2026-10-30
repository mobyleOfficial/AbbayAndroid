package com.mobyle.abbay.data.librivox

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mobyle.abbay.data.R
import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.TrackEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

@HiltWorker
class LibriVoxDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val dao: LibraryDao,
    private val http: LibriVoxHttp,
    private val files: LibriVoxFiles,
    private val tracker: LibriVoxDownloadTracker,
) : CoroutineWorker(context, parameters) {
    private var lastNotificationAt = 0L

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val bookId = inputData.getLong(KEY_BOOK_ID, -1L)
        val details = dao.getBookWithDetails(bookId) ?: return@withContext Result.success()
        val librivoxId = details.book.librivoxId ?: return@withContext Result.success()
        val pending = inListeningOrder(details.tracks.filter { it.remoteUrl != null && !LibriVoxFiles.isLocal(it.documentUri) }, details)
        if (pending.isEmpty()) return@withContext Result.success()

        createChannel()
        val total = details.tracks.size
        var done = total - pending.size
        runCatching { setForeground(foregroundInfo(details.book.title, done, total, 0f)) }

        var failures = 0
        for (track in pending) {
            if (isStopped || dao.getBook(bookId) == null) break
            val succeeded = downloadWithRetries(bookId, librivoxId, track) { fraction ->
                updateNotification(details.book.title, done, total, fraction)
            }
            if (succeeded) done++ else failures++
        }
        when {
            isStopped -> Result.retry()
            failures == 0 -> Result.success()
            runAttemptCount >= MAX_RUN_ATTEMPTS -> Result.failure()
            else -> Result.retry()
        }
    }

    private suspend fun downloadWithRetries(bookId: Long, librivoxId: String, track: TrackEntity, onFraction: (Float) -> Unit): Boolean {
        val remoteUrl = track.remoteUrl ?: return false
        val destination = files.fileFor(librivoxId, remoteUrl) ?: return false.also { tracker.failed(bookId, track.id) }
        tracker.downloading(bookId, track.id, 0f)
        var reportedFraction = 0f
        for (attempt in 0 until ATTEMPTS_PER_CHAPTER) {
            val result = runCatching {
                destination.parentFile?.mkdirs()
                if (!destination.exists()) {
                    http.download(remoteUrl, destination, isStopped = { isStopped }) { received, total ->
                        val fraction = if (total > 0) received.toFloat() / total else expectedFraction(received, track.sizeBytes)
                        if (fraction - reportedFraction >= PROGRESS_STEP) {
                            reportedFraction = fraction
                            tracker.downloading(bookId, track.id, fraction)
                            onFraction(fraction)
                        }
                    }
                }
            }
            if (result.isSuccess) {
                dao.updateTrackUri(track.id, Uri.fromFile(destination).toString())
                tracker.finished(bookId, track.id)
                return true
            }
            val isTransient = result.exceptionOrNull() is IOException
            if (isStopped || !isTransient || attempt == ATTEMPTS_PER_CHAPTER - 1) break
            delay(RETRY_DELAY_MS * (attempt + 1))
        }
        if (isStopped) tracker.finished(bookId, track.id) else tracker.failed(bookId, track.id)
        return false
    }

    private fun inListeningOrder(tracks: List<TrackEntity>, details: BookWithDetails): List<TrackEntity> {
        val sorted = tracks.sortedBy { it.indexInBook }
        val current = details.listeningState?.trackIndex ?: 0
        val (fromCurrent, beforeCurrent) = sorted.partition { it.indexInBook >= current }
        return fromCurrent + beforeCurrent
    }

    private fun expectedFraction(received: Long, expected: Long) = if (expected >
        0
    ) {
        (received.toFloat() / expected).coerceAtMost(1f)
    } else {
        0f
    }

    private fun updateNotification(title: String, done: Int, total: Int, fraction: Float) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationAt < NOTIFICATION_INTERVAL_MS) return
        lastNotificationAt = now
        runCatching { setForegroundAsync(foregroundInfo(title, done, total, fraction)) }
    }

    private fun foregroundInfo(title: String, done: Int, total: Int, fraction: Float): ForegroundInfo {
        val progress = ((done + fraction) / total.coerceAtLeast(1) * PROGRESS_SCALE).toInt()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.librivox_notification_title, title))
            .setContentText(applicationContext.resources.getQuantityString(R.plurals.librivox_notification_text, total, done, total))
            .setProgress(PROGRESS_SCALE, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, applicationContext.getString(R.string.librivox_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val KEY_BOOK_ID = "bookId"
        private const val CHANNEL_ID = "librivox_downloads"
        private const val NOTIFICATION_ID = 4_202
        private const val NOTIFICATION_INTERVAL_MS = 1_000L
        private const val PROGRESS_SCALE = 1_000
        private const val PROGRESS_STEP = 0.01f
        private const val ATTEMPTS_PER_CHAPTER = 3
        private const val RETRY_DELAY_MS = 3_000L
        private const val MAX_RUN_ATTEMPTS = 5
    }
}
