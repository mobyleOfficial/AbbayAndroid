package com.mobyle.abbay.data.chapters

import com.mobyle.abbay.data.scanner.FileKinds

/**
 * Assembles a book's chapter list from the first available source: a CUE sheet,
 * otherwise the chapters embedded in each file, otherwise one chapter per file. Times are book-absolute. Pure logic.
 */
object BookChapterBuilder {
    const val SOURCE_CUE = "CUE"
    const val SOURCE_EMBEDDED = "EMBEDDED"
    const val SOURCE_FILE = "FILE"

    data class Track(val fileName: String, val startInBookMs: Long, val durationMs: Long, val titleTag: String?)

    data class BuiltChapter(val title: String, val startMs: Long, val endMs: Long, val source: String)

    fun build(
        tracks: List<Track>,
        cueChapters: Map<String, List<ParsedChapter>>?,
        embeddedChapters: (Track) -> List<ParsedChapter>,
    ): List<BuiltChapter> {
        if (tracks.isEmpty()) return emptyList()
        val cueByFile = cueChapters?.mapKeys { it.key.substringAfterLast('/').substringAfterLast('\\').lowercase() }
        val useCue = cueByFile != null && tracks.any { it.fileName.lowercase() in cueByFile }

        val starts = tracks.flatMap { track ->
            val inFile = if (useCue) cueByFile?.get(track.fileName.lowercase()).orEmpty() else embeddedChapters(track)
            val source = if (useCue) SOURCE_CUE else SOURCE_EMBEDDED
            val valid = inFile.filter { it.startMs in 0 until maxOf(track.durationMs, 1L) }.sortedBy { it.startMs }
            if (valid.isEmpty()) {
                listOf(Triple(fallbackTitle(track), track.startInBookMs, SOURCE_FILE))
            } else {
                valid.mapIndexed { index, chapter ->
                    // The first chapter of a file starts with the file, so no audio falls outside a chapter.
                    val start = if (index == 0) track.startInBookMs else track.startInBookMs + chapter.startMs
                    Triple(chapter.title, start, source)
                }
            }
        }.distinctBy { it.second }

        val bookEnd = tracks.last().let { it.startInBookMs + it.durationMs }
        return starts.mapIndexed { index, (title, start, source) ->
            val end = starts.getOrNull(index + 1)?.second ?: maxOf(bookEnd, start)
            BuiltChapter(title, start, end, source)
        }
    }

    private fun fallbackTitle(track: Track): String = track.titleTag?.takeIf(String::isNotBlank) ?: FileKinds.baseName(track.fileName)
}
