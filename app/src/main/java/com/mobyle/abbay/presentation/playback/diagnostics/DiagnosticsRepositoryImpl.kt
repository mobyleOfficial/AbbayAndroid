package com.mobyle.abbay.presentation.playback.diagnostics

import android.content.Context
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import com.diagnostics.DiagnosticsReport
import com.repository.DiagnosticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DiagnosticsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : DiagnosticsRepository {
    override suspend fun report(): DiagnosticsReport = withContext(Dispatchers.Default) {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        val decoders = MIME_TYPES.associateWith { mimeType ->
            codecs.filter { !it.isEncoder && it.supportedTypes.any { type -> type.equals(mimeType, ignoreCase = true) } }.map { it.name }
        }
        val opusEncoder = codecs.firstOrNull { it.isEncoder && MediaFormat.MIMETYPE_AUDIO_OPUS in it.supportedTypes }?.name
        val audioManager = context.getSystemService(AudioManager::class.java)
        DiagnosticsReport(
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
            decoders = decoders,
            opusEncoder = opusEncoder,
            outputSampleRate = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull(),
            outputFramesPerBuffer = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull(),
            log = PlaybackLog.buffer.snapshot(),
        )
    }

    private companion object {
        val MIME_TYPES = listOf(
            "audio/mpeg",
            "audio/mp4a-latm",
            "audio/opus",
            "audio/vorbis",
            "audio/flac",
            "audio/raw",
            "audio/3gpp",
            "audio/amr-wb",
        )
    }
}
