package com.mobyle.abbay.infra.readalong

import android.net.Uri
import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongChunk
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongModelStatus
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongTranscriber
import com.readalong.ReadAlongWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-along's engine: an offline recogniser reading the book's own files on the device. No microphone, no
 * network and no speech permission: the audio is already here, and the text is made from it.
 *
 * Runs are serialised: one transcription at a time, so the speech model is never closed under a running
 * recogniser, and the CPU is never spent on two chapters at once.
 */
@Singleton
class VoskReadAlongTranscriber @Inject constructor(
    private val models: ReadAlongModelStore,
    private val decoder: ReadAlongAudioDecoder,
) : ReadAlongTranscriber {
    private val runMutex = Mutex()
    private var cached: Pair<ReadAlongLanguage, Model>? = null

    override fun supportedLanguages(): List<ReadAlongLanguage> = ReadAlongLanguage.entries

    override suspend fun modelStatus(language: ReadAlongLanguage): ReadAlongModelStatus = when {
        ReadAlongModel.forLanguage(language) == null -> ReadAlongModelStatus.UNSUPPORTED
        models.isInstalled(language) -> ReadAlongModelStatus.INSTALLED
        else -> ReadAlongModelStatus.DOWNLOADABLE
    }

    override suspend fun installModel(language: ReadAlongLanguage) {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        models.install(language) {}
    }

    override fun transcribe(source: ReadAlongAudioSource, fromMs: Long, language: ReadAlongLanguage): Flow<ReadAlongChunk> = channelFlow {
        runMutex.withLock {
            val model = modelFor(language)
            for (reading in source.readingsFrom(fromMs)) {
                transcribe(reading, model) { send(it) }
                // The whole file is done: the silence after its last line counts as covered.
                send(ReadAlongChunk(emptyList(), ReadAlongSpan(reading.bookStartMs, reading.bookEndMs)))
            }
        }
    }.flowOn(Dispatchers.Default)

    /** Reads one file from the reading's offset to the segment's end, one chunk per recognised phrase. */
    private suspend fun transcribe(
        reading: ReadAlongAudioSource.Reading,
        model: Model,
        emit: suspend (ReadAlongChunk) -> Unit,
    ) {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        val recognizer = Recognizer(model, TARGET_RATE)
        try {
            recognizer.setWords(true)
            recognizer.setMaxAlternatives(0)
            recognizer.setPartialWords(false)
            var fedMs = reading.bookStartMs
            var reportedMs = fedMs
            decoder.decode(Uri.parse(reading.fileUri), reading.offsetMs, reading.segment.durationMs) { pcm ->
                val phraseEnded = recognizer.acceptWaveForm(pcm, pcm.size)
                fedMs += pcm.size / BYTES_PER_MILLISECOND
                if (phraseEnded) {
                    val line = line(recognizer.result, reading.bookStartMs)
                    if (line != null) {
                        emit(ReadAlongChunk(listOf(line), ReadAlongSpan(reading.bookStartMs, fedMs)))
                        reportedMs = fedMs
                    } else if (fedMs - reportedMs >= PROGRESS_INTERVAL_MS) {
                        emit(ReadAlongChunk(emptyList(), ReadAlongSpan(reading.bookStartMs, fedMs)))
                        reportedMs = fedMs
                    }
                } else if (fedMs - reportedMs >= PROGRESS_INTERVAL_MS) {
                    // A long stretch without a pause: the covered span still has to move, or a run looks stuck.
                    emit(ReadAlongChunk(emptyList(), ReadAlongSpan(reading.bookStartMs, fedMs)))
                    reportedMs = fedMs
                }
            }
            val last = line(recognizer.finalResult, reading.bookStartMs)
            emit(ReadAlongChunk(listOfNotNull(last), ReadAlongSpan(reading.bookStartMs, reading.bookEndMs)))
        } finally {
            recognizer.close()
        }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    /** One line per recognised phrase, one word per timed word, in book time. Nil when nothing was timed. */
    private fun line(json: String, bookStartMs: Long): ReadAlongLine? {
        val words = runCatching { JSONObject(json).optJSONArray("result") }.getOrNull() ?: return null
        val recognised = ArrayList<ReadAlongWord>(words.length())
        for (index in 0 until words.length()) {
            val word = words.optJSONObject(index) ?: continue
            val text = word.optString("word").trim()
            if (text.isEmpty()) continue
            val start = bookStartMs + (word.optDouble("start", 0.0) * MILLIS_PER_SECOND).toLong()
            val end = bookStartMs + (word.optDouble("end", 0.0) * MILLIS_PER_SECOND).toLong()
            recognised.add(ReadAlongWord(text, start, end.coerceAtLeast(start)))
        }
        val first = recognised.firstOrNull() ?: return null
        val last = recognised.last()
        return ReadAlongLine(first.startMs, last.endMs, recognised)
    }

    /**
     * The installed model for the language, loading it on first use. Runs are serialised, so the previous one can
     * be closed here: no recogniser is using it.
     */
    private fun modelFor(language: ReadAlongLanguage): Model {
        cached?.let { (cachedLanguage, model) ->
            if (cachedLanguage == language) return model
            runCatching { model.close() }
            cached = null
        }
        val directory = models.installedFor(language)
            ?: throw ReadAlongEngineException("The ${language.code} speech model is not installed")
        val model = Model(directory.absolutePath)
        cached = language to model
        return model
    }

    private companion object {
        const val TARGET_RATE = 16_000f
        const val MILLIS_PER_SECOND = 1_000.0
        const val BYTES_PER_MILLISECOND = 16_000 * 2 / 1_000L
        const val PROGRESS_INTERVAL_MS = 2_000L
    }
}
