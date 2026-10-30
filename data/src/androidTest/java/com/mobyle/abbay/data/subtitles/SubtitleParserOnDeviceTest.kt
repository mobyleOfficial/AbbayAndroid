package com.mobyle.abbay.data.subtitles

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The parser's patterns compile with Android's ICU regex engine, which is stricter than the JVM's. */
@RunWith(AndroidJUnit4::class)
class SubtitleParserOnDeviceTest {
    @Test
    fun srtAndLrcParseOnTheDevice() {
        assertEquals(1, SubtitleParser.parseSrt("1\n00:00:01,000 --> 00:00:02,000\n{\\an8}<b>Hi</b>\n").size)
        assertEquals(2, SubtitleParser.parseLrc("[offset:100]\n[00:01.00][00:03.00]Hi\n").size)
    }
}
