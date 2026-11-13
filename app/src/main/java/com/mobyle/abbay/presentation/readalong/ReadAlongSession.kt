package com.mobyle.abbay.presentation.readalong

import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongChunk
import com.readalong.ReadAlongDisplay
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongModelStatus
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongState
import com.readalong.ReadAlongTranscriber
import com.readalong.ReadAlongTranscript
import com.usecase.GetReadAlongAudioSource
import com.usecase.GetReadAlongLanguage
import com.usecase.GetReadAlongTranscript
import com.usecase.SaveReadAlongTranscript
import com.usecase.SetReadAlongLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What the session needs from the app: the engine, the saved text, and where the book's audio is. */
class ReadAlongDependencies(
    val transcriber: ReadAlongTranscriber,
    val getTranscript: GetReadAlongTranscript,
    val saveTranscript: SaveReadAlongTranscript,
    val getLanguage: GetReadAlongLanguage,
    val setLanguage: SetReadAlongLanguage,
    val audioSource: GetReadAlongAudioSource,
)

/** What the pane draws at a moment: the lines, and where in them the reader is. */
data class ReadAlongSnapshot(
    val state: ReadAlongState = ReadAlongState.Preparing,
    val language: ReadAlongLanguage? = null,
    val choices: List<ReadAlongLanguage> = emptyList(),
    val lines: List<ReadAlongLine> = emptyList(),
    val display: ReadAlongDisplay = ReadAlongDisplay.Music,
)

/**
 * One book's read-along while its text is on screen: resolves the language, has the model installed when it
 * is not, loads what was saved and transcribes from the shown time, saving as it goes.
 *
 * Transcription runs only while the text is shown (`show` until `stop`), from the shown time ahead of playback.
 * A run starts where the text is uncovered, or at the end of the covered stretch once the shown time is within
 * [LOOKAHEAD_MS] of it, and a seek out of a run's reach restarts it there. A run stops once it is
 * [RUN_AHEAD_MS] past the shown time, so showing the text on a long book doesn't transcribe the rest of it
 * while the audio plays; the next run starts as playback nears where it stopped.
 *
 * One run at a time, cancelled whenever the session retargets. A run never starts inside covered audio, drops
 * incoming lines that start in audio covered before it began (the old lines win), and stops once it reaches it.
 */
@Suppress(
    "TooManyFunctions",
    "ReturnCount",
    "CyclomaticComplexMethod",
    "SwallowedException",
    "TooGenericExceptionCaught",
)
class ReadAlongSession(
    private val dependencies: ReadAlongDependencies,
    private val scope: CoroutineScope,
    private val defaultLanguage: () -> ReadAlongLanguage = { ReadAlongLanguage.ENGLISH },
) {
    private var bookId: String? = null
    private var source: ReadAlongAudioSource? = null
    private var transcript: ReadAlongTranscript? = null
    private var language: ReadAlongLanguage? = null
    private var choices: List<ReadAlongLanguage> = emptyList()

    private var state: ReadAlongState = ReadAlongState.Preparing
    private var shownTimeMs = 0L
    private var isShown = false
    private var isPrepared = false
    private var preparation: Job? = null

    /** Whether a preparation is still running. A cancelled one leaves its field set, so the job's own state counts. */
    private val isPreparing: Boolean get() = preparation?.isActive == true

    /** Language resolved, model installed, saved text loaded: another preparation would only repeat that. */
    private val needsPreparation: Boolean get() = !isPrepared && !isPreparing && !isWaitingForAudio
    private var run: Run? = null
    private var runCount = 0

    /** Bumped on every retarget, so work begun for an earlier book or language drops its result. */
    private var generation = 0
    private var hasFailed = false

    /** The span the last run that ran out of audio started and ended in: another run there would read nothing. */
    private var exhausted: ReadAlongSpan? = null
    private var hasUnsavedText = false
    private var lastSaveAt = 0L
    private var bookDurationMs: Long? = null

    private class Run(val id: Int, val job: Job, val startMs: Long, var frontierMs: Long) {
        /** Whether the run will cover [timeMs] shortly, so a seek there needs no new run. */
        fun reaches(timeMs: Long): Boolean = startMs <= timeMs && timeMs <= frontierMs + LOOKAHEAD_MS
    }

    private enum class RunEnd { FINISHED, ENTERED_COVERED, FAR_ENOUGH_AHEAD, FAILED }

    /** Thrown inside a run's collection to stop the engine as soon as the run is over. */
    private class RunOver(val reason: RunEnd) : Exception()

    /** What the pane shows now. */
    fun snapshot(): ReadAlongSnapshot {
        val current = transcript
        val lines = current?.lines ?: emptyList()
        return ReadAlongSnapshot(
            state = state,
            language = language,
            choices = choices,
            lines = lines,
            display = current?.displayAt(shownTimeMs) ?: ReadAlongDisplay.Music,
        )
    }

    /** The text is on screen for [bookId] at [timeMs]: start, or retarget. The same book at a covered time restarts nothing. */
    fun show(bookId: String, timeMs: Long) {
        if (this.bookId == bookId) {
            isShown = true
            shownTimeMs = timeMs
            if (hasFailed || needsPreparation) {
                hasFailed = false
                prepare()
            } else {
                evaluate()
            }
            return
        }
        stop()
        reset()
        this.bookId = bookId
        shownTimeMs = timeMs
        isShown = true
        prepare()
    }

    /** The shown time moved: playback or a seek. */
    fun timeChanged(timeMs: Long) {
        if (bookId == null || !isShown) return
        shownTimeMs = timeMs
        evaluate()
    }

    /** The book's downloads changed: a chapter may have arrived, or gone. */
    fun downloadsChanged() {
        val bookId = bookId ?: return
        scope.launch {
            val loaded = dependencies.audioSource(bookId) ?: return@launch
            source = loaded
            exhausted = null
            evaluate()
        }
    }

    /** The user picked a language: kept for the book, and its text starts over in it. */
    fun setLanguage(picked: ReadAlongLanguage) {
        val bookId = bookId ?: return
        if (picked == language) return
        cancelWork()
        hasUnsavedText = false
        transcript = null
        language = picked
        isPrepared = false
        hasFailed = false
        exhausted = null
        state = ReadAlongState.Preparing
        scope.launch { dependencies.setLanguage(bookId, picked) }
        prepare()
    }

    /**
     * The text left the screen: the run stops, and what it transcribed is saved. A model download is left to
     * finish, so flipping back to the cover for a moment doesn't throw away 40 MB of it.
     */
    fun stop() {
        isShown = false
        generation++
        cancelRun()
        saveNow()
    }

    /** The reader left the app, or the screen went away for good. */
    fun close() {
        isShown = false
        cancelWork()
    }

    // MARK: - Preparing

    private val isWaitingForAudio: Boolean
        get() = state == ReadAlongState.NeedsDownload || state == ReadAlongState.FileMissing

    private val missingAudioState: ReadAlongState
        get() = if (source?.isRemote == true) ReadAlongState.NeedsDownload else ReadAlongState.FileMissing

    private fun reset() {
        bookId = null
        source = null
        transcript = null
        language = null
        isPrepared = false
        hasFailed = false
        exhausted = null
        hasUnsavedText = false
        isShown = false
        state = ReadAlongState.Preparing
    }

    private fun cancelWork() {
        generation++
        preparation?.cancel()
        preparation = null
        cancelRun()
    }

    private fun prepare() {
        val bookId = bookId ?: return
        if (!isShown) return
        preparation?.cancel()
        generation++
        val generation = generation
        state = ReadAlongState.Preparing
        preparation = scope.launch {
            try {
                prepare(bookId, generation)
            } finally {
                if (isCurrent(generation)) preparation = null
            }
        }
    }

    private fun isCurrent(generation: Int): Boolean = generation == this.generation

    private suspend fun prepare(bookId: String, generation: Int) {
        choices = dependencies.transcriber.supportedLanguages()
        val resolved = language ?: dependencies.getLanguage(bookId) ?: defaultLanguage()
        if (!isCurrent(generation)) return
        language = resolved
        if (!installModel(resolved, generation)) return
        val audio = dependencies.audioSource(bookId)
        if (!isCurrent(generation)) return
        if (audio == null) {
            // The book is gone from the library while its text is up.
            source = null
            isPrepared = false
            state = ReadAlongState.Failed
            return
        }
        source = audio
        val loaded = dependencies.getTranscript(bookId)
        if (!isCurrent(generation)) return
        transcript = loaded?.takeIf { it.language == resolved } ?: ReadAlongTranscript(resolved)
        isPrepared = true
        preparation = null
        evaluate()
    }

    /** Whether the language's model is on the device, installing it first when it isn't. */
    private suspend fun installModel(language: ReadAlongLanguage, generation: Int): Boolean {
        when (dependencies.transcriber.modelStatus(language)) {
            ReadAlongModelStatus.UNSUPPORTED -> {
                if (!isCurrent(generation)) return false
                state = ReadAlongState.Unsupported(language)
                preparation = null
                return false
            }

            ReadAlongModelStatus.DOWNLOADABLE -> {
                if (!isCurrent(generation)) return false
                state = ReadAlongState.DownloadingModel(language)
                try {
                    dependencies.transcriber.installModel(language)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    if (!isCurrent(generation)) return false
                    hasFailed = true
                    state = ReadAlongState.Failed
                    preparation = null
                    return false
                }
                return isCurrent(generation)
            }

            ReadAlongModelStatus.INSTALLED -> return true
        }
    }

    // MARK: - Deciding and running

    /** Brings the state and the run in line with the shown time. */
    private fun evaluate() {
        if (!isShown) return
        val source = source ?: return
        val transcript = transcript
        if (!isPrepared || transcript == null) {
            // The audio may be here now: a chapter that finished downloading.
            if (!isPreparing && isWaitingForAudio && source.isOnDevice(shownTimeMs)) prepare()
            return
        }
        if (hasFailed) return
        if (isPastTheEnd(shownTimeMs)) {
            state = ReadAlongState.Ready
            return
        }
        val covered = transcript.covered.firstOrNull { it.startMs <= shownTimeMs && shownTimeMs < it.endMs }
        if (covered != null) {
            state = ReadAlongState.Ready
            val isNearItsEnd = covered.endMs - shownTimeMs < LOOKAHEAD_MS
            if (isNearItsEnd && canRun(covered.endMs) && run?.reaches(covered.endMs) != true) startRun(covered.endMs)
            return
        }
        evaluateUncovered(shownTimeMs, source)
    }

    /** The shown time has no text yet: the current run is about to reach it, or one starts there. */
    private fun evaluateUncovered(timeMs: Long, source: ReadAlongAudioSource) {
        if (run?.reaches(timeMs) == true) {
            state = ReadAlongState.Preparing
            return
        }
        if (!source.isOnDevice(timeMs)) {
            cancelRun()
            state = missingAudioState
            return
        }
        if (!canRun(timeMs)) {
            state = ReadAlongState.Failed
            return
        }
        state = ReadAlongState.Preparing
        startRun(timeMs)
    }

    private fun isPastTheEnd(timeMs: Long): Boolean {
        val duration = bookDurationMs ?: return false
        return timeMs >= duration
    }

    /** How long the book runs, so a time past its end isn't transcribed. */
    fun setDuration(durationMs: Long) {
        bookDurationMs = durationMs.takeIf { it > 0 }
    }

    private fun canRun(timeMs: Long): Boolean {
        val source = source ?: return false
        if (!source.isOnDevice(timeMs) || isPastTheEnd(timeMs)) return false
        val exhausted = exhausted ?: return true
        return !(exhausted.startMs <= timeMs && timeMs <= exhausted.endMs)
    }

    private fun startRun(startMs: Long) {
        cancelRun()
        val source = source ?: return
        val language = language ?: return
        val transcript = transcript ?: return
        runCount++
        val id = runCount
        // Audio an earlier run covered: its lines win, and this run stops on reaching it.
        val earlier = transcript.covered.toList()
        val limit = earlier.firstOrNull { it.startMs > startMs }?.startMs
        val job = scope.launch {
            var reason = RunEnd.FINISHED
            try {
                dependencies.transcriber.transcribe(source, startMs, language).collect { chunk ->
                    if (run?.id != id) return@collect
                    receive(chunk, earlier, limit)?.let { throw RunOver(it) }
                }
            } catch (over: RunOver) {
                reason = over.reason
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (run?.id == id) reason = RunEnd.FAILED
            }
            endRun(id, reason)
        }
        run = Run(id, job, startMs, startMs)
    }

    private fun cancelRun() {
        run?.job?.cancel()
        run = null
    }

    /** Adds a chunk; how the run ends once it reached audio an earlier run covered or got far enough ahead. */
    private fun receive(chunk: ReadAlongChunk, earlier: List<ReadAlongSpan>, limit: Long?): RunEnd? {
        val transcript = transcript ?: return null
        val fresh = chunk.lines.filter { line -> earlier.none { it.startMs <= line.startMs && line.startMs < it.endMs } }
        transcript.add(fresh, chunk.covered)
        hasUnsavedText = true
        run?.let { current -> current.frontierMs = maxOf(current.frontierMs, chunk.covered.endMs) }
        saveIfDue()
        if (limit != null && chunk.covered.endMs >= limit) return RunEnd.ENTERED_COVERED
        if (chunk.covered.endMs >= shownTimeMs + RUN_AHEAD_MS) return RunEnd.FAR_ENOUGH_AHEAD
        evaluate()
        return null
    }

    private fun endRun(id: Int, end: RunEnd) {
        val ended = run ?: return
        if (ended.id != id) return
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

    // MARK: - Saving

    private fun saveIfDue() {
        val now = System.currentTimeMillis()
        if (now - lastSaveAt < SAVE_INTERVAL_MS) return
        saveNow()
    }

    private fun saveNow() {
        val bookId = bookId ?: return
        val transcript = transcript ?: return
        if (!hasUnsavedText) return
        hasUnsavedText = false
        lastSaveAt = System.currentTimeMillis()
        scope.launch { dependencies.saveTranscript(bookId, transcript) }
    }

    /** The book is gone: its text goes with it, and nothing run from it is written back. */
    fun forget() {
        cancelWork()
        reset()
    }

    companion object {
        /** How close to the end of the covered stretch the shown time gets before the next run starts. */
        const val LOOKAHEAD_MS = 45_000L

        /** How far past the shown time a run transcribes before it stops: minutes of audio, not of listening. */
        const val RUN_AHEAD_MS = 240_000L

        private const val SAVE_INTERVAL_MS = 2_000L
    }
}
