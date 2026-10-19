package com.mobyle.abbay.data.storage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.model.DataError
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Tag and duration reader. Uses the platform `MediaMetadataRetriever`, which reads containers without decoding
 * audio.
 */
class TrackMetadataDataSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun read(documentUri: String): DataResult<TrackMetadata> = withRetriever(documentUri) { retriever ->
        TrackMetadata(
            durationMs = retriever.value(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
            hasAudio = retriever.value(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes",
            album = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            albumArtist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            artist = retriever.value(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            composer = retriever.value(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
            title = retriever.value(MediaMetadataRetriever.METADATA_KEY_TITLE),
            trackNumber = retriever.value(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull(),
        )
    }

    /**
     * Exact duration of an MP3 file that has no Xing/Info/VBRI header, or null when the header-based duration can be
     * trusted. Reads the whole file only in that case.
     */
    fun readExactMp3Duration(documentUri: String): DataResult<Long?> = guardedIo {
        val uri = Uri.parse(documentUri)
        val firstBytes = context.contentResolver.openInputStream(uri)?.use { readUpTo(it, ID3_PROBE_BYTES) } ?: ByteArray(0)
        val head = context.contentResolver.openInputStream(uri)?.use { readUpTo(it, Mp3Duration.headBytesNeeded(firstBytes)) }
            ?: ByteArray(0)
        if (!Mp3Duration.needsFullScan(head)) return@guardedIo null

        val length = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        context.contentResolver.openInputStream(uri)?.use { stream -> Mp3Duration.scan(stream.buffered(), length) }
    }

    fun readEmbeddedPicture(documentUri: String): DataResult<ByteArray?> =
        withRetriever(documentUri) { retriever -> retriever.embeddedPicture }

    /** `InputStream.readNBytes` needs API 33; minSdk is 29. */
    private fun readUpTo(stream: java.io.InputStream, count: Int): ByteArray {
        val buffer = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = stream.read(buffer, filled, count - filled)
            if (read < 0) break
            filled += read
        }
        return buffer.copyOf(filled)
    }

    /** Data sources map every failure to a [DataResult]. */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private inline fun <T> guardedIo(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (securityException: SecurityException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (exception: Exception) {
        DataResult.Failure(DataError.UNREADABLE)
    }

    private fun MediaMetadataRetriever.value(key: Int): String? = extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }

    /** Data sources map every failure to a [DataResult]. */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private inline fun <T> withRetriever(documentUri: String, block: (MediaMetadataRetriever) -> T): DataResult<T> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(documentUri))
            DataResult.Success(block(retriever))
        } catch (securityException: SecurityException) {
            DataResult.Failure(DataError.ACCESS_LOST)
        } catch (exception: Exception) {
            DataResult.Failure(DataError.UNREADABLE)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private companion object {
        const val ID3_PROBE_BYTES = 10
    }
}
