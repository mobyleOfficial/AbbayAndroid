package com.mobyle.abbay.data.readalong

import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongLine
import com.readalong.ReadAlongSpan
import com.readalong.ReadAlongTranscript
import com.readalong.ReadAlongWord
import kotlinx.serialization.Serializable

/** A transcript on disk. Versioned so a later release can migrate rather than drop a whole book's text. */
@Serializable
internal data class ReadAlongTranscriptFile(
    val version: Int = VERSION,
    val language: String,
    val lines: List<ReadAlongLineFile> = emptyList(),
    val covered: List<ReadAlongSpanFile> = emptyList(),
) {
    fun toTranscript(): ReadAlongTranscript = ReadAlongTranscript.of(
        language = ReadAlongLanguage.ofCode(language),
        lines = lines.map { it.toLine() },
        covered = covered.map { ReadAlongSpan(it.startMs, it.endMs) },
    )

    companion object {
        const val VERSION = 1

        fun of(transcript: ReadAlongTranscript): ReadAlongTranscriptFile = ReadAlongTranscriptFile(
            language = transcript.language.code,
            lines = transcript.lines.map(ReadAlongLineFile::of),
            covered = transcript.covered.map { ReadAlongSpanFile(it.startMs, it.endMs) },
        )
    }
}

@Serializable
internal data class ReadAlongLineFile(val startMs: Long, val endMs: Long, val words: List<ReadAlongWordFile>) {
    fun toLine(): ReadAlongLine = ReadAlongLine(startMs, endMs, words.map { ReadAlongWord(it.text, it.startMs, it.endMs) })

    companion object {
        fun of(line: ReadAlongLine): ReadAlongLineFile =
            ReadAlongLineFile(line.startMs, line.endMs, line.words.map { ReadAlongWordFile(it.text, it.startMs, it.endMs) })
    }
}

@Serializable
internal data class ReadAlongWordFile(val text: String, val startMs: Long, val endMs: Long)

@Serializable
internal data class ReadAlongSpanFile(val startMs: Long, val endMs: Long)
