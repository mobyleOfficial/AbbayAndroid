package com.mobyle.abbay.data.converter

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.converter.ConversionEstimate
import com.converter.ConversionPlan
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.repository.ConverterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConverterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runner: ConversionRunner,
    private val transcoder: OpusTranscoder,
    private val settings: SettingsDataSource,
) : ConverterRepository {
    private val encoder by lazy { transcoder.encoderName() }

    override fun encoderName(): String? = encoder

    override suspend fun plan(scope: ConversionScope, options: ConverterOptions): ConversionPlan = withContext(Dispatchers.IO) {
        val tracks = runner.jobsOf(scope).map { it.second }
        ConversionPlan(
            fileCount = tracks.size,
            totalBytes = tracks.sumOf { it.sizeBytes },
            estimatedSavingBytes = tracks.sumOf { ConversionEstimate.saving(it.sizeBytes, it.durationMs, options.bitrateKbps) },
        )
    }

    /**
     * "Battery not low … or charging; storage not low"; the user's minimum level is checked per file. Not
     * expedited: WorkManager allows only network and storage constraints on expedited work.
     */
    override fun start(scope: ConversionScope, options: ConverterOptions) {
        val request = OneTimeWorkRequestBuilder<ConverterWorker>()
            .setInputData(ConverterWorker.inputOf(scope, options))
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_MINUTES, TimeUnit.MINUTES)
            .build()
        runner.progress.value = ConversionProgress(isRunning = true)
        WorkManager.getInstance(context).enqueueUniqueWork(ConverterWorker.UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(ConverterWorker.UNIQUE_NAME)
    }

    override fun observeProgress(): Flow<ConversionProgress> = runner.progress

    override fun observeTotalSaved(): Flow<Long> = settings.observeSettings().map { it.converterSavedBytes }.distinctUntilChanged()

    private companion object {
        const val RETRY_MINUTES = 30L
    }
}
