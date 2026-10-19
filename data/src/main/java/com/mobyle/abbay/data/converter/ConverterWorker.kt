package com.mobyle.abbay.data.converter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.converter.ConversionEnd
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.mobyle.abbay.data.R
import com.repository.LibraryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The converter's WorkManager job, in the foreground with a progress notification. The
 * scope and options travel in the input data; the job ends early when the battery is below the minimum (it is retried
 * later) and can be cancelled from the converter screen.
 */
@HiltWorker
class ConverterWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val runner: ConversionRunner,
    private val libraryRepository: LibraryRepository,
) : CoroutineWorker(context, parameters) {
    private var lastNotificationAt = 0L

    override suspend fun doWork(): Result {
        val scope = scopeOf(inputData.getString(KEY_BOOK_ID), inputData.getLong(KEY_ROOT_ID, -1L)) ?: return Result.failure()
        val options = ConverterOptions(
            bitrateKbps = inputData.getInt(KEY_BITRATE, ConverterOptions.DEFAULT_BITRATE),
            forceMono = inputData.getBoolean(KEY_MONO, false),
            deleteOriginals = inputData.getBoolean(KEY_DELETE_ORIGINALS, false),
        )
        createChannel()
        setForeground(foregroundInfo(ConversionProgress(isRunning = true)))
        val end = runner.run(scope, options, isStopped = { isStopped }) { progress -> updateNotification(progress) }
        // New .opus files replace the MP3s: rescan so the library points at them (same book rows and positions).
        libraryRepository.fullScan()
        return if (end == ConversionEnd.BATTERY_LOW) Result.retry() else Result.success()
    }

    /** Called from the transcoding thread: the asynchronous form, at most once a second. */
    private fun updateNotification(progress: ConversionProgress) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationAt < NOTIFICATION_INTERVAL_MS && progress.isRunning) return
        lastNotificationAt = now
        runCatching { setForegroundAsync(foregroundInfo(progress)) }
    }

    private fun foregroundInfo(progress: ConversionProgress): ForegroundInfo {
        val saving = Formatter.formatShortFileSize(applicationContext, progress.estimatedSavingBytes)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.converter_notification_title))
            .setContentText(
                applicationContext.resources.getQuantityString(
                    R.plurals.converter_notification_text,
                    progress.filesTotal,
                    progress.filesDone,
                    progress.filesTotal,
                    saving,
                ),
            )
            .setProgress(
                PROGRESS_SCALE,
                (
                    (progress.filesDone + progress.fileFraction) / progress.filesTotal.coerceAtLeast(1) *
                        PROGRESS_SCALE
                    ).toInt(),
                false
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, applicationContext.getString(R.string.converter_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val UNIQUE_NAME = "opus-converter"
        private const val KEY_BOOK_ID = "bookId"
        private const val KEY_ROOT_ID = "rootId"
        private const val KEY_BITRATE = "bitrate"
        private const val KEY_MONO = "mono"
        private const val KEY_DELETE_ORIGINALS = "deleteOriginals"
        private const val CHANNEL_ID = "converter"
        private const val NOTIFICATION_ID = 4_201
        private const val NOTIFICATION_INTERVAL_MS = 1_000L
        private const val PROGRESS_SCALE = 1_000

        fun inputOf(scope: ConversionScope, options: ConverterOptions) = workDataOf(
            KEY_BOOK_ID to (scope as? ConversionScope.Book)?.bookId,
            KEY_ROOT_ID to ((scope as? ConversionScope.Root)?.rootId ?: -1L),
            KEY_BITRATE to options.bitrateKbps,
            KEY_MONO to options.forceMono,
            KEY_DELETE_ORIGINALS to options.deleteOriginals,
        )

        private fun scopeOf(bookId: String?, rootId: Long): ConversionScope? = when {
            bookId != null -> ConversionScope.Book(bookId)
            rootId >= 0 -> ConversionScope.Root(rootId)
            else -> null
        }
    }
}
