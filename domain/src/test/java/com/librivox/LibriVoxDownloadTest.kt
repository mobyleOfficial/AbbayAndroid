package com.librivox

import com.model.ChapterDownload
import com.model.ChapterDownloadStatus
import com.model.LibriVoxDownload
import com.model.NetworkStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibriVoxDownloadTest {
    @Test
    fun progressIsWeightedByEachChaptersSize() {
        val download = LibriVoxDownload.of(
            listOf(ChapterDownload(ChapterDownloadStatus.DOWNLOADED), ChapterDownload(ChapterDownloadStatus.DOWNLOADING, 0.5f)),
            expectedBytes = listOf(300L, 100L),
        )
        assertEquals(0.875f, download.fraction, 0.0001f)
        assertFalse(download.isComplete)
    }

    @Test
    fun withoutSizesProgressCountsChapters() {
        val download = LibriVoxDownload.of(
            listOf(ChapterDownload(ChapterDownloadStatus.DOWNLOADED), ChapterDownload(ChapterDownloadStatus.FAILED)),
            expectedBytes = listOf(0L, 0L),
        )
        assertEquals(0.5f, download.fraction, 0.0001f)
        assertTrue(download.hasFailures)
    }

    @Test
    fun smallBooksDownloadAnywhereAndLargeOnesAskOnMeteredNetworks() {
        val metered = NetworkStatus(isOnline = true, isMetered = true)
        val wifi = NetworkStatus(isOnline = true, isMetered = false)
        val large = CellularDownloadPolicy.LARGE_DOWNLOAD_BYTES + 1

        assertEquals(CellularDownloadPolicy.Decision.Proceed(allowsCellular = true), CellularDownloadPolicy.decide(1_000L, metered))
        assertEquals(CellularDownloadPolicy.Decision.AskUser, CellularDownloadPolicy.decide(large, metered))
        assertEquals(CellularDownloadPolicy.Decision.Proceed(allowsCellular = false), CellularDownloadPolicy.decide(large, wifi))
    }
}
