package com.mobyle.abbay.data.readalong

import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongChunk
import com.readalong.ReadAlongLineBreaker
import com.readalong.ReadAlongModelStatus
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongTranscriber
import com.readalong.ReadAlongWord
import com.readalong.UnsupportedReadAlongLocaleException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Read-along's engine: Vosk, offline in the app, reading the book's own files through [ReadAlongAudioDecoder]. No
 * microphone and nothing sent anywhere; the only network use is the one-time model download ([VoskModelInstaller]).
 *
 * Only final results are used: a line is shown once and never rewritten. Audio counts as covered once a result has
 * been returned for it (an utterance, or a long silence, which Vosk closes as an empty result), so a run cut off mid
 * utterance leaves that audio uncovered and the next run reads it again.
 *
 * One model is loaded at a time, and only while text is wanted: [release] frees it after [RELEASE_DELAY_MS], so the
 * pill flipping back and forth doesn't reload it each time. The engine lock keeps a model from being freed under a run.
 */
@Singleton
class VoskReadAlongTranscriber @Inject constructor(
    private val installer: VoskModelInstaller,
    private val decoder: ReadAlongAudioDecoder,
) : ReadAlongTranscriber {
    private val engineLock = Mutex()
    private var loaded: Pair<String, Model>? = null
    private val housekeeping = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingRelease: Job? = null

    init {
        runCatching { LibVosk.setLogLevel(LogLevel.WARNINGS) }
    }

    override fun supportedLocales(): List<String> = VoskModels.all.map { it.locale }.sorted()

    override suspend fun installedLocales(): List<String> = withContext(Dispatchers.IO) { installer.installedLocales() }

    override suspend fun modelStatus(locale: String): ReadAlongModelStatus {
        val model = VoskModels.forLocale(locale) ?: return ReadAlongModelStatus.UNSUPPORTED
        val isInstalled = withContext(Dispatchers.IO) { installer.isInstalled(model.locale) }
        return if (isInstalled) ReadAlongModelStatus.INSTALLED else ReadAlongModelStatus.DOWNLOADABLE
    }

    override suspend fun installModel(locale: String, onProgress: (Float) -> Unit) {
        val model = VoskModels.forLocale(locale) ?: throw UnsupportedReadAlongLocaleException(locale)
        installer.install(model.locale, onProgress)
    }

    override suspend fun discardModelDownload(locale: String) = installer.discardDownload(locale)

    override fun transcribe(source: ReadAlongAudioSource, startMs: Long, locale: String): Flow<ReadAlongChunk> = channelFlow {
        val context = currentCoroutineContext()
        withModel(locale) { model ->
            for (reading in source.readings(startMs)) {
                if (!context.isActive) return@withModel
                transcribe(reading, model, context) { chunk -> channel.trySendBlocking(chunk) }
            }
        }
    }.buffer(CHUNK_BUFFER).flowOn(Dispatchers.IO)

    /** Reads one file from the reading's offset to its segment's end, handing over a chunk per result. */
    private fun transcribe(
        reading: ReadAlongAudioSource.Reading,
        model: Model,
        context: CoroutineContext,
        send: (ReadAlongChunk) -> Unit,
    ) {
        val uri = reading.segment.uri ?: return
        Recognizer(model, ReadAlongAudioDecoder.SPEECH_RATE.toFloat()).use { recognizer ->
            recognizer.setWords(true)
            var fedSamples = 0L
            decoder.decode(uri, reading.offsetMs, reading.bookEndMs - reading.bookStartMs, { !context.isActive }) { samples ->
                fedSamples += samples.size
                if (recognizer.acceptWaveForm(samples, samples.size)) {
                    val coveredEnd = minOf(reading.bookStartMs + samplesToMs(fedSamples), reading.bookEndMs)
                    send(chunk(recognizer.result, reading, coveredEnd))
                }
            }
            if (!context.isActive) return
            // The whole file is read: what follows its last word is covered too.
            send(chunk(recognizer.finalResult, reading, reading.bookEndMs))
        }
    }

    override suspend fun confidence(source: ReadAlongAudioSource, startMs: Long, durationMs: Long, locale: String): Double =
        withContext(Dispatchers.IO) {
            val reading = source.readings(startMs).firstOrNull() ?: return@withContext 0.0
            val uri = reading.segment.uri ?: return@withContext 0.0
            val context = currentCoroutineContext()
            withModel(locale) { model ->
                val confidences = ArrayList<Double>()
                Recognizer(model, ReadAlongAudioDecoder.SPEECH_RATE.toFloat()).use { recognizer ->
                    recognizer.setWords(true)
                    decoder.decode(uri, reading.offsetMs, durationMs, { !context.isActive }) { samples ->
                        if (recognizer.acceptWaveForm(samples, samples.size)) confidences += wordsOf(recognizer.result).map { it.conf }
                    }
                    confidences += wordsOf(recognizer.finalResult).map { it.conf }
                }
                if (confidences.isEmpty()) 0.0 else confidences.average()
            }
        }

    override fun release() {
        pendingRelease?.cancel()
        pendingRelease = housekeeping.launch {
            delay(RELEASE_DELAY_MS)
            engineLock.withLock {
                loaded?.second?.close()
                loaded = null
            }
        }
    }

    /** Runs [block] with the locale's model loaded, holding the engine so the model can't be freed meanwhile. */
    private suspend fun <T> withModel(locale: String, block: (Model) -> T): T {
        pendingRelease?.cancel()
        val wanted = VoskModels.forLocale(locale) ?: throw UnsupportedReadAlongLocaleException(locale)
        return engineLock.withLock {
            val current = loaded
            val model = if (current?.first == wanted.locale) {
                current.second
            } else {
                current?.second?.close()
                loaded = null
                if (!installer.isInstalled(wanted.locale)) throw UnsupportedReadAlongLocaleException(locale)
                Model(installer.folder(wanted.locale).absolutePath).also { loaded = wanted.locale to it }
            }
            block(model)
        }
    }

    private fun chunk(json: String, reading: ReadAlongAudioSource.Reading, coveredEndMs: Long): ReadAlongChunk {
        val words = wordsOf(json).mapNotNull { word ->
            val text = word.word.trim()
            if (text.isEmpty() || text in UNKNOWN_WORDS) return@mapNotNull null
            val start = reading.bookStartMs + secondsToMs(word.start)
            ReadAlongWord(text, start, maxOf(start, reading.bookStartMs + secondsToMs(word.end)))
        }
        val lines = ReadAlongLineBreaker.lines(words).mapNotNull { it.clipped(reading.bookEndMs) }
        return ReadAlongChunk(lines, ReadAlongSpan(reading.bookStartMs, maxOf(coveredEndMs, reading.bookStartMs)))
    }

    private fun wordsOf(json: String): List<VoskWord> = runCatching { parser.decodeFromString(VoskResult.serializer(), json).result }
        .getOrDefault(emptyList())

    @Serializable
    internal data class VoskResult(@SerialName("result") val result: List<VoskWord> = emptyList())

    @Serializable
    internal data class VoskWord(
        @SerialName("word") val word: String,
        @SerialName("start") val start: Double,
        @SerialName("end") val end: Double,
        @SerialName("conf") val conf: Double = 1.0,
    )

    private companion object {
        const val RELEASE_DELAY_MS = 30_000L
        const val CHUNK_BUFFER = 16
        const val MILLIS_PER_SECOND = 1_000L
        val UNKNOWN_WORDS = setOf("[unk]", "<unk>")
        val parser = Json { ignoreUnknownKeys = true }

        fun samplesToMs(samples: Long): Long = samples * MILLIS_PER_SECOND / ReadAlongAudioDecoder.SPEECH_RATE

        fun secondsToMs(seconds: Double): Long = (seconds * MILLIS_PER_SECOND).toLong()
    }
}
