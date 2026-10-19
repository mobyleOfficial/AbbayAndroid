package com.mobyle.abbay.data.subtitles

import com.subtitles.SubtitleLine
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * `.srt` and `.lrc` parsing and the encoding rule. Pure Kotlin. Literal `]`
 * and `}` in the patterns are escaped: Android's ICU regex engine rejects them bare (the JVM accepts them).
 */
object SubtitleParser {
    private val srtTime = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->""")
    private val lrcTime = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")
    private val lrcOffset = Regex("""^\[offset:\s*([+-]?\d+)\]""", RegexOption.IGNORE_CASE)
    private val markup = Regex("""<[^>]+>|\{\\[^}]*\}""")

    fun parse(fileName: String, text: String): List<SubtitleLine> =
        if (fileName.lowercase().endsWith(".lrc")) parseLrc(text) else parseSrt(text)

    /** Blocks separated by blank lines: optional index, `start --> end`, then the text lines (tags removed). */
    fun parseSrt(text: String): List<SubtitleLine> {
        val lines = mutableListOf<SubtitleLine>()
        text.replace("\r\n", "\n").replace('\r', '\n').split(Regex("\n\\s*\n")).forEach { block ->
            val rows = block.trim().lines()
            val timeRow = rows.indexOfFirst { srtTime.containsMatchIn(it) }
            if (timeRow < 0) return@forEach
            val match = srtTime.find(rows[timeRow]) ?: return@forEach
            val (hours, minutes, seconds, fraction) = match.destructured
            val startMs = ((hours.toLong() * MINUTES_PER_HOUR + minutes.toLong()) * SECONDS_PER_MINUTE + seconds.toLong()) * MS +
                millisOf(fraction)
            val content = rows.drop(timeRow + 1).joinToString("\n") { it.replace(markup, "").trim() }.trim()
            if (content.isNotEmpty()) lines += SubtitleLine(startMs, content)
        }
        return lines.sortedBy { it.startMs }
    }

    /** `[mm:ss.xx] text` lines; one line may carry several times; `[offset:±ms]` shifts every time. */
    fun parseLrc(text: String): List<SubtitleLine> {
        var offsetMs = 0L
        val lines = mutableListOf<SubtitleLine>()
        text.lines().forEach { raw ->
            val row = raw.trim()
            lrcOffset.find(row)?.let { offsetMs = it.groupValues[1].toLong() }
            val times = generateSequence(lrcTime.matchAt(row, 0)) { previous -> lrcTime.matchAt(row, previous.range.last + 1) }.toList()
            if (times.isEmpty()) return@forEach
            val content = row.substring(times.last().range.last + 1).replace(markup, "").trim()
            if (content.isEmpty()) return@forEach
            times.forEach { match ->
                val (minutes, seconds, fraction) = match.destructured
                val startMs = (minutes.toLong() * SECONDS_PER_MINUTE + seconds.toLong()) * MS + millisOf(fraction)
                // A positive offset makes the lyrics appear sooner.
                lines += SubtitleLine((startMs - offsetMs).coerceAtLeast(0L), content)
            }
        }
        return lines.sortedBy { it.startMs }
    }

    /**
     * UTF-8 (with or without BOM), else UTF-16 with BOM, else [chosenCharset]; null when none of these applies, so the
     * user has to choose.
     */
    fun decode(bytes: ByteArray, chosenCharset: String?): String? = when {
        bytes.startsWith(UTF8_BOM) -> String(bytes, UTF8_BOM.size, bytes.size - UTF8_BOM.size, Charsets.UTF_8)
        bytes.startsWith(UTF16_LE_BOM) || bytes.startsWith(UTF16_BE_BOM) -> String(bytes, Charsets.UTF_16)
        else -> strictly(bytes, Charsets.UTF_8)
            ?: chosenCharset?.let { name -> runCatching { Charset.forName(name) }.getOrNull() }?.let { String(bytes, it) }
    }

    private fun strictly(bytes: ByteArray, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

    private fun millisOf(fraction: String): Long = when (fraction.length) {
        0 -> 0L
        1 -> fraction.toLong() * HUNDREDS
        2 -> fraction.toLong() * TENS
        else -> fraction.take(MILLIS_DIGITS).toLong()
    }

    private fun ByteArray.startsWith(prefix: ByteArray) = size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
    private const val MS = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
    private const val HUNDREDS = 100L
    private const val TENS = 10L
    private const val MILLIS_DIGITS = 3
}
