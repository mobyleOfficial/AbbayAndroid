package com.mobyle.abbay.data.audiobookshelf

import com.mobyle.abbay.data.librivox.LibriVoxHttp
import com.model.AudiobookshelfLibrary
import com.model.LibriVoxBook
import javax.inject.Inject
import javax.inject.Singleton

/** Blocking calls to the signed-in Audiobookshelf server; callers move them off the main thread. */
@Singleton
class AudiobookshelfApi @Inject constructor(
    private val http: LibriVoxHttp,
    private val session: AudiobookshelfSession,
    private val auth: AudiobookshelfAuth,
) {
    fun libraries(): List<AudiobookshelfLibrary> = AudiobookshelfJson.libraries(get("/api/libraries"))

    fun recentlyAdded(libraryId: String): List<LibriVoxBook> {
        val body = get(
            "/api/libraries/${AudiobookshelfUrls.segment(libraryId)}/items",
            mapOf("sort" to "addedAt", "desc" to "1", "limit" to RECENT_ROWS.toString(), "page" to "0", "minified" to "1"),
        )
        return AudiobookshelfJson.pageItems(body, ::coverUrlOf)
    }

    fun search(libraryId: String, query: String): List<LibriVoxBook> {
        val body = get(
            "/api/libraries/${AudiobookshelfUrls.segment(libraryId)}/search",
            mapOf("q" to query, "limit" to SEARCH_ROWS.toString()),
        )
        return AudiobookshelfJson.searchItems(body, ::coverUrlOf)
    }

    fun book(itemId: String): LibriVoxBook? = auth.authorized { serverUrl, access ->
        val body = http.getText("$serverUrl/api/items/${AudiobookshelfUrls.segment(itemId)}", mapOf("expanded" to "1"), access = access)
        AudiobookshelfJson.book(body, serverUrl, ::coverUrlOf)
    }

    fun coverBytes(book: LibriVoxBook): ByteArray? = runCatching {
        auth.authorized { _, access -> http.getBytes(book.coverUrl, MAX_COVER_BYTES, access) }
    }.getOrNull()

    fun coverUrlOf(itemId: String): String {
        val serverUrl = session.credentials()?.serverUrl ?: session.account.value?.serverUrl ?: return ""
        return AudiobookshelfUrls.coverUrl(serverUrl, itemId)
    }

    private fun get(path: String, query: Map<String, String> = emptyMap()): String = auth.authorized { serverUrl, access ->
        http.getText(serverUrl + path, query, access = access)
    }

    private companion object {
        const val RECENT_ROWS = 40
        const val SEARCH_ROWS = 40
        const val MAX_COVER_BYTES = 4 * 1024 * 1024
    }
}
