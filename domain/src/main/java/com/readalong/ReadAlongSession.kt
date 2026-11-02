package com.readalong

import com.model.Book
import com.repository.ReadAlongRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the text shows: the state, the transcript so far, the language in use and the languages that can be picked. */
data class ReadAlongSnapshot(
    val state: ReadAlongState = ReadAlongState.Preparing,
    val transcript: ReadAlongTranscript? = null,
    val language: String? = null,
    val choices: List<String> = emptyList(),
)

/**
 * One book's read-along while its text is on screen: resolves the language, has the model downloaded when needed, says
 * when the audio isn't on the device, loads what was saved and transcribes from the shown time, saving as it goes.
 *
 * Transcription runs only while the text is shown ([show] until [stop]), ahead of playback. A run starts where the text
 * is uncovered, or at the end of the covered stretch once the shown time is within [LOOKAHEAD_MS] of it; a seek out of a
 * run's reach restarts it there. A run stops [RUN_AHEAD_MS] past the shown time, so a long book isn't transcribed to
 * its end while it plays. While a finger drags the scrubber nothing starts: the run starts where the finger lifts.
 *
 * One run at a time, cancelled whenever the session retargets. Two runs never transcribe the same audio (each cuts
 * phrases differently, so both sets of lines would be kept side by side): a run never starts inside covered audio,
 * drops incoming lines that start in audio covered before it began, and stops on reaching that audio.
 *
 * Every method must be called from [scope]'s single thread (the main thread in the app), where its work runs too; the
 * engine and the repository move their own work off it.
 */
@Suppress("TooManyFunctions")
class ReadAlongSession(
    private val scope: CoroutineScope,
    private val transcriber: ReadAlongTranscriber,
    private val repository: ReadAlongRepository,
    /** The device's languages, most preferred first ("pt-BR", "en-US"). */
    private val preferredLanguages: () -> List<String>,
    /** The app's language tag ("pt-BR"), the system's when the app follows it. */
    private val appLanguage: () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val saveIntervalMs: Long = SAVE_INTERVAL_MS,
) {
    private val mutableSnapshot = MutableStateFlow(ReadAlongSnapshot())
    val snapshot: StateFlow<ReadAlongSnapshot> = mutableSnapshot.asStateFlow()

    private class Run(val id: Int, val startMs: Long, var job: Job? = null, var frontierMs: Long = startMs) {
        /** Whether the run will cover [timeMs] shortly, so the shown time there needs no new run. */
        fun reaches(timeMs: Long) = timeMs >= startMs && timeMs <= frontierMs + LOOKAHEAD_MS
    }

    private enum class RunEnd { FINISHED, ENTERED_COVERED, FAR_ENOUGH_AHEAD, FAILED }

    private var book: Book? = null
    private var source: ReadAlongAudioSource? = null
    private var shownTimeMs = 0L
    private var isShown = false
    private var isPrepared = false
    private var preparation: Job? = null
    private var run: Run? = null
    private var runCount = 0
    private var generation = 0
    private var hasFailed = false
    private var exhausted: ReadAlongSpan? = null
    private var hasUnsavedText = false
    private var lastSaveAt: Long? = null
    private var saveStamp: Int? = null
    private var isScrubbing = false

    private var state: ReadAlongState
        get() = mutableSnapshot.value.state
        set(value) = mutableSnapshot.update { it.copy(state = value) }

    private var transcript: ReadAlongTranscript?
        get() = mutableSnapshot.value.transcript
        set(value) = mutableSnapshot.update { it.copy(transcript = value) }

    private var currentLanguage: String?
        get() = mutableSnapshot.value.language
        set(value) = mutableSnapshot.update { it.copy(language = value) }

    /** The text is on screen for [shownBook] at [timeMs]: start, or retarget. The same book changes only the time. */
    fun show(shownBook: Book, timeMs: Long) {
        val isSameBook = book?.id == shownBook.id
        if (!isSameBook) {
            stop()
            reset()
        }
        book = shownBook
        shownTimeMs = timeMs
        isShown = true
        if (!isSameBook || hasFailed || isIdleUnprepared) {
            hasFailed = false
            prepare()
        } else {
            evaluate()
        }
    }

    /** The shown time moved: playback, a seek, or the scrubber ([scrubbing] while the finger is down). */
    fun timeChanged(timeMs: Long, scrubbing: Boolean = false) {
        if (book == null || !isShown) return
        shownTimeMs = timeMs
        isScrubbing = scrubbing
        evaluate()
    }

    /** The book's files changed: a chapter may have finished downloading, or gone. */
    fun downloadsChanged(changedBook: Book) {
        if (book?.id != changedBook.id) return
        book = changedBook
        exhausted = null
        val currentGeneration = generation
        scope.launch {
            val updated = repository.audioSource(changedBook)
            if (book?.id == changedBook.id && currentGeneration == generation) {
                source = updated
                evaluate()
            }
        }
    }

    /** The user picked a language: kept for the book, and its text starts over in it. */
    fun setLanguage(locale: String) {
        val bookId = book?.id
        if (bookId == null || locale == currentLanguage) return
        cancelWork()
        hasUnsavedText = false
        saveStamp = null
        transcript = null
        currentLanguage = locale
        isPrepared = false
        hasFailed = false
        exhausted = null
        state = ReadAlongState.Preparing
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.setLanguage(bookId, locale)
            repository.delete(bookId)
        }
        prepare()
    }

    /** The text left the screen: the run stops, and what it transcribed is saved. */
    fun stop() {
        isShown = false
        isScrubbing = false
        cancelWork()
        saveNow()
        transcriber.release()
    }

    /** Read-along was turned off: everything stops at once and the text is forgotten (the caller deletes the files). */
    fun disable() {
        cancelWork()
        reset()
        transcriber.release()
    }

    private val isWaitingForAudio: Boolean
        get() = state == ReadAlongState.NeedsDownload || state == ReadAlongState.FileMissing

    /** Nothing prepared and nothing under way: neither preparing now nor waiting for the audio to arrive. */
    private val isIdleUnprepared: Boolean
        get() = !isPrepared && preparation == null && !isWaitingForAudio

    private val missingAudioState: ReadAlongState
        get() = if (source?.isDownloadable == true) ReadAlongState.NeedsDownload else ReadAlongState.FileMissing

    private fun reset() {
        book = null
        source = null
        isPrepared = false
        hasFailed = false
        exhausted = null
        hasUnsavedText = false
        lastSaveAt = null
        saveStamp = null
        isScrubbing = false
        isShown = false
        mutableSnapshot.value = ReadAlongSnapshot()
    }

    private fun cancelWork() {
        generation += 1
        preparation?.cancel()
        preparation = null
        cancelRun()
    }

    private fun isCurrent(expected: Int) = expected == generation

    private fun prepare() {
        val preparedBook = book
        if (preparedBook == null || !isShown) return
        preparation?.cancel()
        generation += 1
        val currentGeneration = generation
        state = ReadAlongState.Preparing
        preparation = scope.launch {
            val failure = runCatching { prepare(preparedBook, currentGeneration) }.exceptionOrNull()
            if (failure is CancellationException) throw failure
            if (failure != null && isCurrent(currentGeneration)) fail()
        }
    }

    private fun fail() {
        hasFailed = true
        state = ReadAlongState.Failed
        preparation = null
    }

    /** Language, model, saved text: once all three are in place, runs can start. */
    private suspend fun prepare(preparedBook: Book, currentGeneration: Int) {
        val locale = resolvedLocale(preparedBook, currentGeneration)
        when {
            locale == null -> if (isCurrent(currentGeneration)) preparation = null
            installModel(locale, currentGeneration) && isCurrent(currentGeneration) -> loadText(preparedBook, locale, currentGeneration)
        }
    }

    /** Where the audio is, which languages can be picked, and the one the book is read in; null when it can't be known yet. */
    private suspend fun resolvedLocale(preparedBook: Book, currentGeneration: Int): String? {
        val loadedSource = repository.audioSource(preparedBook)
        if (!isCurrent(currentGeneration)) return null
        source = loadedSource
        val supported = transcriber.supportedLocales()
        mutableSnapshot.update { it.copy(choices = supported) }
        val locale = currentLanguage ?: resolveLanguage(preparedBook, supported, loadedSource, currentGeneration)
        return locale?.takeIf { isCurrent(currentGeneration) }?.also { currentLanguage = it }
    }

    private suspend fun loadText(preparedBook: Book, locale: String, currentGeneration: Int) {
        val loaded = repository.load(preparedBook.id)
        if (!isCurrent(currentGeneration)) return
        transcript = loaded.transcript?.takeIf { it.language == locale } ?: ReadAlongTranscript(locale)
        saveStamp = loaded.stamp
        isPrepared = true
        preparation = null
        evaluate()
    }

    /** Whether the locale's model is on the device, downloading it first when it can be. When not, says why. */
    private suspend fun installModel(locale: String, currentGeneration: Int): Boolean = when (transcriber.modelStatus(locale)) {
        ReadAlongModelStatus.INSTALLED -> true
        ReadAlongModelStatus.DOWNLOADABLE -> downloadModel(locale, currentGeneration)
        ReadAlongModelStatus.UNSUPPORTED -> {
            if (isCurrent(currentGeneration)) {
                state = ReadAlongState.Unsupported(locale)
                preparation = null
            }
            false
        }
    }

    private suspend fun downloadModel(locale: String, currentGeneration: Int): Boolean {
        if (!isCurrent(currentGeneration)) return false
        state = ReadAlongState.DownloadingModel(locale, null)
        val failure = runCatching {
            transcriber.installModel(locale) { progress ->
                scope.launch {
                    if (isCurrent(currentGeneration) && state is ReadAlongState.DownloadingModel) {
                        state = ReadAlongState.DownloadingModel(locale, progress)
                    }
                }
            }
        }.exceptionOrNull()
        if (failure is CancellationException) throw failure
        if (failure != null && isCurrent(currentGeneration)) fail()
        return failure == null
    }

    /**
     * The book's language: its own setting wins (a bare code, from LibriVox, is matched to a supported locale and saved
     * back as one); else the one its EPUB declares; else a short sample is scored with each installed candidate; else
     * the app's language.
     */
    private suspend fun resolveLanguage(
        resolvedBook: Book,
        supported: List<String>,
        resolvedSource: ReadAlongAudioSource,
        currentGeneration: Int,
    ): String? {
        val stored = repository.language(resolvedBook.id) ?: declaredLanguage(resolvedBook, supported)
        return if (stored != null) {
            matchedStoredLanguage(resolvedBook, stored, supported)
        } else {
            detectedLanguage(resolvedBook, supported, resolvedSource, currentGeneration)
        }
    }

    /** The language the book's EPUB declares, as a bare code ("en-GB" is "en"), when a supported locale speaks it. */
    private suspend fun declaredLanguage(resolvedBook: Book, supported: List<String>): String? {
        val declared = repository.bookText(resolvedBook)?.language?.let(ReadAlongLanguage::languageOf) ?: return null
        return declared.takeIf { code -> supported.any { ReadAlongLanguage.languageOf(it) == code } }
    }

    private suspend fun matchedStoredLanguage(resolvedBook: Book, stored: String, supported: List<String>): String {
        if (!ReadAlongLanguage.isBareCode(stored)) return stored
        val installed = transcriber.installedLocales()
        val matched = ReadAlongLanguage.locale(stored, supported, installed, preferredRegion(stored))
        if (matched != null) repository.setLanguage(resolvedBook.id, matched)
        return matched ?: stored
    }

    /** Detection reads the audio, so without it this waits (null), saying the audio is missing. The result is kept for the book. */
    private suspend fun detectedLanguage(
        resolvedBook: Book,
        supported: List<String>,
        resolvedSource: ReadAlongAudioSource,
        currentGeneration: Int,
    ): String? {
        if (!resolvedSource.isOnDevice(shownTimeMs)) {
            if (isCurrent(currentGeneration)) state = missingAudioState
            return null
        }
        val installed = transcriber.installedLocales()
        val candidates = ReadAlongLanguage.candidates(appLanguage(), preferredLanguages(), installed)
        val scores = scores(resolvedSource, candidates, currentGeneration) ?: return null
        val locale = ReadAlongLanguage.best(scores) ?: appLanguageLocale(supported, installed)
        repository.setLanguage(resolvedBook.id, locale)
        return locale
    }

    /** Each candidate's confidence on a sample of the audio; a candidate the engine can't score has none. Null once retargeted. */
    private suspend fun scores(
        resolvedSource: ReadAlongAudioSource,
        candidates: List<String>,
        currentGeneration: Int
    ): Map<String, Double>? {
        val scores = HashMap<String, Double>()
        for (candidate in candidates) {
            if (!isCurrent(currentGeneration)) break
            val result = runCatching { transcriber.confidence(resolvedSource, shownTimeMs, DETECTION_MS, candidate) }
            (result.exceptionOrNull() as? CancellationException)?.let { throw it }
            result.getOrNull()?.let { scores[candidate] = it }
        }
        return scores.takeIf { isCurrent(currentGeneration) }
    }

    /** The app language's supported locale; the tag itself (which then reads as unsupported) when there is none. */
    private fun appLanguageLocale(supported: List<String>, installed: List<String>): String {
        val tag = appLanguage().replace('_', '-')
        val code = ReadAlongLanguage.languageOf(tag)
        val region = tag.split('-').getOrNull(1)?.takeIf { it.length == REGION_LENGTH } ?: preferredRegion(code)
        return ReadAlongLanguage.locale(code, supported, installed, region) ?: tag
    }

    /** The region of the first device language in [code]: "pt" on a Brazilian device is "BR". */
    private fun preferredRegion(code: String): String? = preferredLanguages()
        .map { it.replace('_', '-').split('-') }
        .firstOrNull { it.first().equals(code, ignoreCase = true) && it.getOrNull(1)?.length == REGION_LENGTH }
        ?.get(1)

    /** Brings the state and the run in line with the shown time. */
    private fun evaluate() {
        val currentSource = source
        if (!isShown || currentSource == null) return
        val currentTranscript = transcript
        when {
            !isPrepared || currentTranscript == null -> prepareIfAudioArrived(currentSource)
            hasFailed -> Unit
            isPastTheEnd(shownTimeMs) -> state = ReadAlongState.Ready
            else -> currentTranscript.coveringSpan(shownTimeMs)?.let(::evaluateCovered) ?: evaluateUncovered(shownTimeMs, currentSource)
        }
    }

    /** Detection waits for the audio: it may be here now. */
    private fun prepareIfAudioArrived(currentSource: ReadAlongAudioSource) {
        if (preparation == null && isWaitingForAudio && currentSource.isOnDevice(shownTimeMs)) prepare()
    }

    /** The shown time has text: once it nears the end of its stretch, the next run starts there. */
    private fun evaluateCovered(span: ReadAlongSpan) {
        state = ReadAlongState.Ready
        val isNearItsEnd = !isScrubbing && span.endMs - shownTimeMs < LOOKAHEAD_MS
        if (isNearItsEnd && canRun(span.endMs) && run?.reaches(span.endMs) != true) startRun(span.endMs)
    }

    /** The shown time has no text yet: the current run is about to reach it, or one starts there. */
    private fun evaluateUncovered(time: Long, currentSource: ReadAlongAudioSource) {
        when {
            run?.reaches(time) == true -> state = ReadAlongState.Preparing
            !currentSource.isOnDevice(time) -> {
                if (!isScrubbing) cancelRun()
                state = missingAudioState
            }
            isScrubbing -> state = ReadAlongState.Preparing
            !canRun(time) -> state = ReadAlongState.Failed
            else -> {
                state = ReadAlongState.Preparing
                startRun(time)
            }
        }
    }

    private fun isPastTheEnd(time: Long): Boolean = book?.duration?.let { duration -> duration in 1..time } == true

    private fun canRun(time: Long): Boolean {
        val spent = exhausted
        val isSpent = spent != null && time >= spent.startMs && time <= spent.endMs
        return source?.isOnDevice(time) == true && !isPastTheEnd(time) && !isSpent
    }

    /** What a run reads: the audio, the language, and the stretches covered before it began. */
    private class RunInputs(val source: ReadAlongAudioSource, val locale: String, val earlier: List<ReadAlongSpan>)

    private fun runInputs(): RunInputs? {
        val audio = source?.takeIf { isShown }
        val locale = currentLanguage
        val covered = transcript?.covered
        return if (audio != null && locale != null && covered != null) RunInputs(audio, locale, covered) else null
    }

    private fun startRun(startMs: Long) {
        cancelRun()
        val inputs = runInputs() ?: return
        runCount += 1
        val started = Run(runCount, startMs)
        // Audio an earlier run covered: its lines win, and this run stops on reaching it.
        val limit = inputs.earlier.firstOrNull { it.startMs > startMs }?.startMs
        run = started
        started.job = scope.launch { endRun(started, collectRun(started, inputs, limit)) }
    }

    /** Takes the run's chunks until it ends one way or another; collecting stops cancels the engine's reading. */
    private suspend fun collectRun(started: Run, inputs: RunInputs, limit: Long?): RunEnd {
        var ending: RunEnd? = null
        val result = runCatching {
            transcriber.transcribe(inputs.source, started.startMs, inputs.locale)
                .takeWhile { chunk ->
                    if (run === started) ending = receive(chunk, started, inputs.earlier, limit)
                    run === started && ending == null
                }
                .collect()
        }
        (result.exceptionOrNull() as? CancellationException)?.let { throw it }
        return if (result.isFailure) RunEnd.FAILED else ending ?: RunEnd.FINISHED
    }

    private fun cancelRun() {
        run?.job?.cancel()
        run = null
    }

    /** Adds a chunk; how the run ends once it reached earlier-covered audio or got far enough ahead, null while it goes on. */
    private fun receive(chunk: ReadAlongChunk, started: Run, earlier: List<ReadAlongSpan>, limit: Long?): RunEnd? {
        val current = transcript ?: return null
        val fresh = chunk.lines.filter { line -> earlier.none { line.startMs in it } }
        transcript = current.adding(fresh, chunk.covered)
        hasUnsavedText = true
        started.frontierMs = maxOf(started.frontierMs, chunk.covered.endMs)
        saveIfDue()
        val end = when {
            limit != null && chunk.covered.endMs >= limit -> RunEnd.ENTERED_COVERED
            chunk.covered.endMs >= shownTimeMs + RUN_AHEAD_MS -> RunEnd.FAR_ENOUGH_AHEAD
            else -> null
        }
        if (end == null) evaluate()
        return end
    }

    private fun endRun(ended: Run, end: RunEnd) {
        if (run !== ended) return
        run = null
        saveNow()
        when (end) {
            RunEnd.FAILED -> {
                hasFailed = true
                state = ReadAlongState.Failed
            }
            RunEnd.FINISHED -> {
                exhausted = ReadAlongSpan(ended.startMs, ended.frontierMs)
                evaluate()
            }
            RunEnd.ENTERED_COVERED, RunEnd.FAR_ENOUGH_AHEAD -> evaluate()
        }
    }

    /** Within a run: at most every [saveIntervalMs], so a fast run doesn't rewrite the file for every chunk. */
    private fun saveIfDue() {
        val last = lastSaveAt
        if (last != null && clock() - last < saveIntervalMs) return
        saveNow()
    }

    /** Saves carry the stamp the text was loaded with, so a book deleted meanwhile stays deleted. */
    private fun saveNow() {
        val bookId = book?.id?.takeIf { hasUnsavedText }
        val text = transcript
        val stamp = saveStamp
        if (bookId == null || text == null || stamp == null) return
        hasUnsavedText = false
        lastSaveAt = clock()
        scope.launch(start = CoroutineStart.UNDISPATCHED) { repository.save(bookId, text, stamp) }
    }

    companion object {
        /** How close to the end of the covered stretch the shown time gets before the next run starts. */
        const val LOOKAHEAD_MS = 60_000L

        /** How far past the shown time a run transcribes before it stops: five minutes of audio. */
        const val RUN_AHEAD_MS = 300_000L

        /** Audio each candidate language transcribes when detecting a book's language. */
        const val DETECTION_MS = 20_000L

        const val SAVE_INTERVAL_MS = 2_000L

        private const val REGION_LENGTH = 2
    }
}
