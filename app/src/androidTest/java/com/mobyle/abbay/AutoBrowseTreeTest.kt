package com.mobyle.abbay

import android.content.ComponentName
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AutoBrowseTreeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var browser: MediaBrowser

    @Before
    fun connect() {
        val token = SessionToken(context, ComponentName(context, "com.mobyle.abbay.presentation.playback.PlaybackService"))
        browser =
            MediaBrowser.Builder(context, token).setApplicationLooper(Looper.getMainLooper()).buildAsync().get(TIMEOUT, TimeUnit.SECONDS)
    }

    @After
    fun release() = instrumentation.runOnMainSync { browser.release() }

    @Test
    fun rootListsTheFiveFolders() {
        val root = onMain { browser.getLibraryRoot(null) }.value!!
        val folders = onMain { browser.getChildren(root.mediaId, 0, Int.MAX_VALUE, null) }.value!!
        assertEquals(listOf("continue", "inProgress", "new", "finished", "bookmarks"), folders.map { it.mediaId })
        assertTrue(folders.all { it.mediaMetadata.isBrowsable == true && it.mediaMetadata.isPlayable == false })
    }

    @Test
    fun booksArePlayableAndCoversAreServedByTheApp() {
        val books = listOf("continue", "inProgress", "new", "finished").flatMap { folder ->
            onMain { browser.getChildren(folder, 0, Int.MAX_VALUE, null) }.value.orEmpty()
        }
        assertTrue(onMain { browser.getChildren("continue", 0, Int.MAX_VALUE, null) }.value.orEmpty().size <= 10)
        books.forEach { book ->
            assertTrue(book.mediaId.startsWith("book/"))
            assertEquals(true, book.mediaMetadata.isPlayable)
            assertEquals(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK, book.mediaMetadata.mediaType)
            book.mediaMetadata.artworkUri?.let { assertEquals("${context.packageName}.covers", it.authority) }
        }
    }

    @Test
    fun aBookChosenByIdOrBySearchBecomesItsPlaylist() {
        val book = listOf("continue", "inProgress", "new", "finished").firstNotNullOfOrNull { folder ->
            onMain { browser.getChildren(folder, 0, Int.MAX_VALUE, null) }.value?.firstOrNull()
        } ?: return
        runOnMain { browser.setMediaItem(MediaItem.Builder().setMediaId(book.mediaId).build()) }
        waitUntil { runOnMain { browser.mediaItemCount } > 0 }

        val title = book.mediaMetadata.title.toString()
        onMain { browser.search(title, null) }
        val results = onMain { browser.getSearchResult(title, 0, Int.MAX_VALUE, null) }.value.orEmpty()
        assertEquals(book.mediaId, results.first().mediaId)
    }

    private fun <T> onMain(block: () -> com.google.common.util.concurrent.ListenableFuture<T>): T {
        var future: com.google.common.util.concurrent.ListenableFuture<T>? = null
        instrumentation.runOnMainSync { future = block() }
        return future!!.get(TIMEOUT, TimeUnit.SECONDS)
    }

    private fun <T> runOnMain(block: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT * 1_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(POLL_MS)
        assertTrue(condition())
    }

    private companion object {
        const val TIMEOUT = 10L
        const val POLL_MS = 100L
    }
}
