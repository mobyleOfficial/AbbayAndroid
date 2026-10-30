package com.mobyle.abbay.data.converter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.converter.ConversionEnd
import com.converter.ConversionEstimate
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.mobyle.abbay.data.database.dao.LibraryDao
import com.mobyle.abbay.data.database.entity.BookEntity
import com.mobyle.abbay.data.database.entity.TrackEntity
import com.mobyle.abbay.data.library.BookProjection
import com.mobyle.abbay.data.scanner.FileKinds
import com.mobyle.abbay.data.settings.SettingsDataSource
import com.mobyle.abbay.data.storage.TrackMetadataDataSource
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ConversionRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LibraryDao,
    private val files: ConverterFileSource,
    private val transcoder: OpusTranscoder,
    private val metadata: TrackMetadataDataSource,
    private val settings: SettingsDataSource,
) {
    val progress = MutableStateFlow(ConversionProgress())

    private class Job(val book: BookEntity, val treeUri: String, val track: TrackEntity)

    suspend fun jobsOf(scope: ConversionScope): List<Pair<BookEntity, TrackEntity>> {
        val books = when (scope) {
            is ConversionScope.Book -> listOfNotNull(scope.bookId.toLongOrNull()?.let { dao.getBook(it) })
            is ConversionScope.Root -> dao.getBooksOfRoot(scope.rootId)
        }
        return books.filter { !it.isSingleFile && it.rootId != null && !it.hidden }.flatMap { book ->
            val details = dao.getBookWithDetails(book.id) ?: return@flatMap emptyList()
            BookProjection.playableTracks(details).filter { it.fileName.lowercase().endsWith(MP3_EXTENSION) }.map { book to it }
        }
    }

    suspend fun run(
        scope: ConversionScope,
        options: ConverterOptions,
        isStopped: () -> Boolean,
        onUpdate: (ConversionProgress) -> Unit,
    ): ConversionEnd {
        val roots = dao.getRoots().associateBy { it.id }
        val jobs = jobsOf(scope).mapNotNull { (book, track) ->
            roots[book.rootId]?.takeIf { it.isAccessible }?.let { Job(book, it.treeUri, track) }
        }
        val estimate = jobs.sumOf { ConversionEstimate.saving(it.track.sizeBytes, it.track.durationMs, options.bitrateKbps) }
        publish(ConversionProgress(isRunning = true, filesTotal = jobs.size, estimatedSavingBytes = estimate), onUpdate)
        val copyCover = settings.observeSettings().first().converterCopyCover
        var end: ConversionEnd? = null
        val remaining = jobs.withIndex().iterator()
        while (end == null && remaining.hasNext()) {
            val (index, job) = remaining.next()
            end = stopReason(isStopped) ?: convertAndRecord(index, job, options, copyCover, isStopped, onUpdate)
        }
        val result =
            end ?: if (jobs.isNotEmpty() && progress.value.failedFiles == jobs.size) ConversionEnd.FAILED else ConversionEnd.FINISHED
        publish(progress.value.copy(isRunning = false, fileName = null, end = result), onUpdate)
        return result
    }

    private suspend fun stopReason(isStopped: () -> Boolean): ConversionEnd? = when {
        isStopped() -> ConversionEnd.CANCELLED
        !hasEnoughBattery() -> ConversionEnd.BATTERY_LOW
        else -> null
    }

    @Suppress("LongParameterList")
    private suspend fun convertAndRecord(
        index: Int,
        job: Job,
        options: ConverterOptions,
        copyCover: Boolean,
        isStopped: () -> Boolean,
        onUpdate: (ConversionProgress) -> Unit,
    ): ConversionEnd? {
        publish(progress.value.copy(fileName = job.track.fileName, filesDone = index, fileFraction = 0f), onUpdate)
        val saved = runCatching { convert(job, options, copyCover, isStopped, onUpdate) }
            .onFailure { Log.w(LOG_TAG, "Conversion failed", it) }
            .getOrNull()
        if (saved == null && isStopped()) return ConversionEnd.CANCELLED
        val current = progress.value
        publish(
            current.copy(
                filesDone = index + 1,
                savedBytes = current.savedBytes + (saved ?: 0L),
                failedFiles = current.failedFiles + if (saved == null) 1 else 0,
            ),
            onUpdate,
        )
        saved?.let { bytes -> settings.updateSettings { it.copy(converterSavedBytes = it.converterSavedBytes + bytes) } }
        return null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun convert(
        job: Job,
        options: ConverterOptions,
        copyCover: Boolean,
        isStopped: () -> Boolean,
        onUpdate: (ConversionProgress) -> Unit
    ): Long {
        val located = files.locate(job.treeUri, job.book.folderDocumentId, job.track.documentUri) ?: error("Track not found")
        val baseName = FileKinds.baseName(job.track.fileName)
        val part = files.createFresh(job.treeUri, located.parentDocumentId, "$baseName$PART_SUFFIX")
        var renamed: android.net.Uri? = null
        try {
            val result = files.openOutput(part).use { output ->
                transcoder.transcode(
                    sourceUri = job.track.documentUri,
                    output = output,
                    bitrateKbps = options.bitrateKbps,
                    forceMono = options.forceMono,
                    tags = tagsOf(job.track, copyCover),
                    onProgress = { fraction -> publish(progress.value.copy(fileFraction = fraction), onUpdate) },
                    isCancelled = isStopped,
                )
            }
            val (mimeType, durationUs) = files.probe(part)
            check(mimeType == OPUS_MIME) { "Not an Opus file" }
            check(abs(durationUs - result.decodedDurationUs) <= MAXIMUM_DURATION_DIFFERENCE_US) { "Duration mismatch" }
            files.children(job.treeUri, located.parentDocumentId).firstOrNull { it.name == "$baseName$OPUS_EXTENSION" }
                ?.let { files.delete(job.treeUri, it.documentId) }
            val opus = files.rename(part, "$baseName$OPUS_EXTENSION").also { renamed = it }
            val saved = located.entry.sizeBytes - files.sizeOf(opus)
            if (options.deleteOriginals) {
                files.delete(job.treeUri, located.entry.documentId)
            } else {
                files.moveToOriginals(job.treeUri, job.book.folderDocumentId, located)
            }
            return saved
        } catch (failure: Throwable) {
            runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, renamed ?: part) }
            throw failure
        }
    }

    private fun tagsOf(track: TrackEntity, copyCover: Boolean): OpusTags {
        val tags = (metadata.read(track.documentUri) as? DataResult.Success)?.value
        val comments = listOfNotNull(
            tags?.title?.let { "TITLE" to it },
            tags?.album?.let { "ALBUM" to it },
            tags?.artist?.let { "ARTIST" to it },
            tags?.albumArtist?.let { "ALBUMARTIST" to it },
            tags?.trackNumber?.let { "TRACKNUMBER" to it.toString() },
        )
        val picture = if (copyCover) (metadata.readEmbeddedPicture(track.documentUri) as? DataResult.Success)?.value else null
        return OpusTags(comments, picture?.takeIf { it.isNotEmpty() }?.let { OpusTags.Cover(mimeOf(it), it) })
    }

    @Suppress("MagicNumber")
    private fun mimeOf(image: ByteArray): String =
        if (image.size > 3 && image[0] == 0x89.toByte() && image[1] == 'P'.code.toByte()) "image/png" else "image/jpeg"

    private suspend fun hasEnoughBattery(): Boolean {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return true
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return isCharging ||
            level < 0 ||
            scale <= 0 ||
            level * PERCENT / scale >= settings.observeSettings().first().converterMinimumBatteryPercent
    }

    private fun publish(value: ConversionProgress, onUpdate: (ConversionProgress) -> Unit) {
        progress.value = value
        onUpdate(value)
    }

    private companion object {
        const val LOG_TAG = "Converter"
        const val MP3_EXTENSION = ".mp3"
        const val OPUS_EXTENSION = ".opus"
        const val PART_SUFFIX = ".opus.part"
        const val OPUS_MIME = "audio/opus"
        const val MAXIMUM_DURATION_DIFFERENCE_US = 500_000L
        const val PERCENT = 100
    }
}
