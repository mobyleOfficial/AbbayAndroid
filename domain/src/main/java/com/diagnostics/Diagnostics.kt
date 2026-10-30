package com.diagnostics

data class LogLine(val atMs: Long, val message: String, val fileName: String? = null)

class LogRingBuffer(private val capacity: Int = CAPACITY) {
    private val lines = ArrayDeque<LogLine>()

    @Synchronized
    fun add(line: LogLine) {
        lines.addLast(line)
        while (lines.size > capacity) lines.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<LogLine> = lines.toList()

    companion object {
        const val CAPACITY = 200
    }
}

data class DiagnosticsReport(
    val androidVersion: String,
    val device: String,
    val appVersion: String,
    val decoders: Map<String, List<String>>,
    val opusEncoder: String?,
    val outputSampleRate: Int?,
    val outputFramesPerBuffer: Int?,
    val log: List<LogLine>,
)

object DiagnosticsText {
    const val HIDDEN_FILE_NAME = "<file>"

    fun of(report: DiagnosticsReport, includeFileNames: Boolean, formatTime: (Long) -> String): String = buildString {
        appendLine("Abbay ${report.appVersion}")
        appendLine("Android ${report.androidVersion} · ${report.device}")
        appendLine("Audio output: ${report.outputSampleRate ?: "?"} Hz, ${report.outputFramesPerBuffer ?: "?"} frames per buffer")
        appendLine("Opus encoder: ${report.opusEncoder ?: "none"}")
        appendLine()
        appendLine("Decoders")
        report.decoders.forEach { (mimeType, names) -> appendLine("  $mimeType: ${names.ifEmpty { listOf("none") }.joinToString()}") }
        appendLine()
        appendLine("Playback log (last ${report.log.size} lines)")
        report.log.forEach { line -> appendLine("  ${formatTime(line.atMs)} ${lineText(line, includeFileNames)}") }
    }

    fun lineText(line: LogLine, includeFileNames: Boolean): String {
        val fileName = line.fileName ?: return line.message
        return "${line.message} ${if (includeFileNames) fileName else HIDDEN_FILE_NAME}"
    }
}
