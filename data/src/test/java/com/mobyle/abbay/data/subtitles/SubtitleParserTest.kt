package com.mobyle.abbay.data.subtitles

import com.subtitles.SubtitleLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleParserTest {
    @Test
    fun srtBlocksWithIndexesTagsAndCrLf() {
        val text =
            "1\r\n00:00:01,500 --> 00:00:03,000\r\n<i>Hello</i>\r\nthere\r\n\r\n2\r\n00:01:02.25 --> 00:01:04,000\r\nSecond\r\n\r\n3\r\n" +
                "bad line\r\n"
        assertEquals(listOf(SubtitleLine(1_500, "Hello\nthere"), SubtitleLine(62_250, "Second")), SubtitleParser.parseSrt(text))
    }

    @Test
    fun lrcWithSeveralTimesMetadataAndOffset() {
        val text = "[ar:Someone]\n[offset:+500]\n[00:10.00][01:00.50]Chorus\n[00:05.1] Verse\n[00:20.00]\n"
        assertEquals(
            listOf(SubtitleLine(4_600, "Verse"), SubtitleLine(9_500, "Chorus"), SubtitleLine(60_000, "Chorus")),
            SubtitleParser.parseLrc(text),
        )
    }

    @Test
    fun encodingIsUtf8OrUtf16WithBomOrTheChosenCharset() {
        val word = "Ação"
        assertEquals(word, SubtitleParser.decode(word.toByteArray(Charsets.UTF_8), null))
        assertEquals(word, SubtitleParser.decode(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + word.toByteArray(), null))
        assertEquals(word, SubtitleParser.decode(word.toByteArray(Charsets.UTF_16), null))
        val latin = word.toByteArray(Charsets.ISO_8859_1)
        assertNull(SubtitleParser.decode(latin, null))
        assertEquals(word, SubtitleParser.decode(latin, "windows-1252"))
    }
}
