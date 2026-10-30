package com.mobyle.abbay.data.chapters

/**
 * CUE sheets (`FILE` / `TRACK` / `TITLE` / `INDEX 01`, times `mm:ss:ff` with 75 frames per second).
 * Returns chapters grouped by the audio file name each `FILE` names.
 */
object CueSheetParser {
    private const val FRAMES_PER_SECOND = 75
    private val timePattern = Regex("""(\d+):(\d{1,2}):(\d{1,2})""")

    fun parse(text: String): Map<String, List<ParsedChapter>> {
        val chapters = linkedMapOf<String, MutableList<ParsedChapter>>()
        var currentFile: String? = null
        var trackTitle: String? = null
        var trackNumber = 0

        text.lineSequence().map { it.trim() }.forEach { line ->
            val keyword = line.substringBefore(' ').uppercase()
            val argument = line.substringAfter(' ', "").trim()
            when (keyword) {
                "FILE" -> {
                    currentFile = unquote(argument.substringBeforeLast(' ').ifEmpty { argument })
                    trackTitle = null
                }

                "TRACK" -> {
                    trackNumber = argument.substringBefore(' ').toIntOrNull() ?: (trackNumber + 1)
                    trackTitle = null
                }

                "TITLE" -> if (trackNumber > 0) trackTitle = unquote(argument)

                "INDEX" -> {
                    val (indexNumber, time) = argument.split(Regex("\\s+")).let { it.getOrNull(0) to it.getOrNull(1) }
                    val file = currentFile
                    val startMs = time?.let(::parseTime)
                    if (indexNumber?.toIntOrNull() == 1 && file != null && startMs != null) {
                        chapters.getOrPut(file) { mutableListOf() } += ParsedChapter(trackTitle.orEmpty(), startMs)
                    }
                }
            }
        }
        return chapters
    }

    private fun parseTime(time: String): Long? {
        val match = timePattern.matchEntire(time) ?: return null
        val (minutes, seconds, frames) = match.destructured
        return (minutes.toLong() * 60 + seconds.toLong()) * 1_000 + frames.toLong() * 1_000 / FRAMES_PER_SECOND
    }

    private fun unquote(value: String): String = value.trim().removeSurrounding("\"")
}
