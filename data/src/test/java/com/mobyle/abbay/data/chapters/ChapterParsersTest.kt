package com.mobyle.abbay.data.chapters

import com.mobyle.abbay.data.chapters.SyntheticFiles.chap
import com.mobyle.abbay.data.chapters.SyntheticFiles.ctoc
import com.mobyle.abbay.data.chapters.SyntheticFiles.id3Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterParsersTest {
    private val expected = listOf(ParsedChapter("Prologue", 0), ParsedChapter("The Storm", 61_500), ParsedChapter("Épilogue", 3_600_250))

    @Test
    fun `cue sheet tracks per file with 75 frames per second`() {
        val cue = """
            REM GENRE Audiobook
            TITLE "Stories"
            FILE "part1.mp3" MP3
              TRACK 01 AUDIO
                TITLE "Prologue"
                INDEX 00 00:00:00
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "The Storm"
                INDEX 01 01:01:37
            FILE "part2.mp3" MP3
              TRACK 03 AUDIO
                INDEX 01 60:00:18
        """.trimIndent()

        val chapters = CueSheetParser.parse(cue)

        assertEquals(listOf(ParsedChapter("Prologue", 0), ParsedChapter("The Storm", 61_493)), chapters["part1.mp3"])
        assertEquals(listOf(ParsedChapter("", 3_600_240)), chapters["part2.mp3"])
    }

    @Test
    fun `mp4 nero chpl box`() {
        val file = SyntheticFiles.concat(
            SyntheticFiles.box("ftyp", "M4B ".toByteArray()),
            SyntheticFiles.box("moov", SyntheticFiles.box("udta", SyntheticFiles.neroChpl(expected.map { it.startMs to it.title }))),
        )
        assertEquals(expected, Mp4ChapterParser.parse(ByteArraySource(file)))
    }

    @Test
    fun `mp4 quicktime chapter text track`() {
        val file = SyntheticFiles.quickTimeChapterFile(
            listOf("Prologue" to 61_500L, "The Storm" to 3_538_750L, "Épilogue" to 10_000L),
            timescale = 600,
        )
        assertEquals(expected, Mp4ChapterParser.parse(ByteArraySource(file)))
    }

    @Test
    fun `mp4 quicktime track wins over nero and a file without chapters has none`() {
        assertTrue(Mp4ChapterParser.parse(ByteArraySource(SyntheticFiles.box("moov", SyntheticFiles.box("trak")))).isEmpty())
        assertTrue(Mp4ChapterParser.parse(ByteArraySource(ByteArray(64))).isEmpty())
    }

    @Test
    fun `id3v2_4 chap frames ordered by the top-level ctoc`() {
        val tag = id3Tag(
            4,
            chap("c2", 3_600_250, 3_700_000, "Épilogue", 4),
            chap("c0", 0, 61_500, "Prologue", 4),
            chap("c1", 61_500, 3_600_250, "The Storm", 4),
            ctoc(listOf("c0", "c1", "c2"), 4),
        )
        assertEquals(expected, Id3ChapterParser.parse(ByteArraySource(tag + ByteArray(100))))
    }

    @Test
    fun `id3v2_3 chap frames without ctoc are sorted by start and decode utf-16 and latin-1`() {
        val tag = id3Tag(
            3,
            chap("b", 61_500, 3_600_250, "The Storm", 3, encoding = 0),
            chap("a", 0, 61_500, "Prologue", 3, encoding = 1),
            chap("c", 3_600_250, 3_700_000, "Épilogue", 3, encoding = 1),
        )
        assertEquals(expected, Id3ChapterParser.parse(ByteArraySource(tag)))
    }

    @Test
    fun `mp3 without id3 tag has no chapters`() {
        assertTrue(Id3ChapterParser.parse(ByteArraySource(byteArrayOf(0xFF.toByte(), 0xFB.toByte()) + ByteArray(100))).isEmpty())
    }

    @Test
    fun `matroska chapters through the seek head and by scanning`() {
        val chapters = expected.map { it.startMs to it.title }
        assertEquals(expected, MatroskaChapterParser.parse(ByteArraySource(SyntheticFiles.matroska(chapters, withSeekHead = true))))
        assertEquals(expected, MatroskaChapterParser.parse(ByteArraySource(SyntheticFiles.matroska(chapters, withSeekHead = false))))
    }

    private val vorbisComments = listOf(
        "TITLE=Stories",
        "CHAPTER002=00:01:01.5",
        "CHAPTER002NAME=The Storm",
        "CHAPTER001=00:00:00.000",
        "CHAPTER001NAME=Prologue",
        "chapter003=01:00:00.250",
        "CHAPTER003NAME=Épilogue",
    )

    @Test
    fun `vorbis comment chapters in opus, vorbis and flac`() {
        assertEquals(expected, VorbisCommentChapterParser.parse(ByteArraySource(SyntheticFiles.oggOpus(vorbisComments))))
        assertEquals(expected, VorbisCommentChapterParser.parse(ByteArraySource(SyntheticFiles.oggVorbis(vorbisComments))))
        assertEquals(expected, VorbisCommentChapterParser.parse(ByteArraySource(SyntheticFiles.flac(vorbisComments))))
    }

    @Test
    fun `the reader picks the parser by extension`() {
        val opus = SyntheticFiles.oggOpus(vorbisComments)
        assertEquals(expected, ChapterReader.embeddedChapters("book.OPUS", ByteArraySource(opus)))
        assertTrue(ChapterReader.embeddedChapters("book.wav", ByteArraySource(opus)).isEmpty())
    }
}
