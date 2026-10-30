package com.mobyle.abbay.data.backup

import com.mobyle.abbay.data.sidecar.SidecarBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupCodecTest {
    @Test
    fun entriesRoundTripWithPathAndHistory() {
        val entry = BackupCodec.Entry(
            book = SidecarBook(
                id = "sidecar-1",
                updatedAt = 1_700_000_000_000L,
                position = SidecarBook.Position("02.mp3", 4_000L, 14_000L),
                state = "IN_PROGRESS",
                lastPlayedAt = 1_700_000_000_000L,
                lastPausedAt = null,
                bookmarks = listOf(SidecarBook.Bookmark(3_000L, "One", "note", 1_600_000_000_000L)),
            ),
            relativePath = "Fantasy/The Long Road",
            title = "The Long Road",
            history = listOf(BackupCodec.HistoryItem("SEEK", 10L, 0L, 30_000L), BackupCodec.HistoryItem("PLAY", 5L, 0L, null)),
        )

        assertEquals(listOf(entry), BackupCodec.read(BackupCodec.write(listOf(entry), exportedAt = 1L)))
    }

    @Test
    fun aSidecarIsNotABackup() {
        assertNull(BackupCodec.read("{\"format\": \"abbay-book\", \"id\": \"x\"}"))
        assertNull(BackupCodec.read("[]"))
    }
}
