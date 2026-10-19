package com.mobyle.abbay.data.chapters

/** Picks the embedded-chapter parser for a file by its extension (source 2). */
object ChapterReader {
    fun embeddedChapters(fileName: String, source: ByteSource): List<ParsedChapter> =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "m4a", "m4b", "mp4", "aac", "3gp" -> Mp4ChapterParser.parse(source)
            "mp3", "mp2", "mpga" -> Id3ChapterParser.parse(source)
            "mka", "webm", "weba" -> MatroskaChapterParser.parse(source)
            "ogg", "oga", "opus", "flac" -> VorbisCommentChapterParser.parse(source)
            else -> emptyList()
        }
}
