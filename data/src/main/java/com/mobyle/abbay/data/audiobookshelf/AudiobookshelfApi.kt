package com.mobyle.abbay.data.audiobookshelf

import com.mobyle.abbay.data.librivox.LibriVoxHttp
import com.model.AudiobookshelfLibrary
import com.model.AudiobookshelfPage
import com.model.AudiobookshelfSeries
import com.model.AudiobookshelfSort
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

    fun books(libraryId: String, sort: AudiobookshelfSort, page: Int): AudiobookshelfPage<LibriVoxBook> {
        val (field, isDescending) = when (sort) {
            AudiobookshelfSort.RECENTLY_ADDED -> "addedAt" to true
            AudiobookshelfSort.TITLE -> "media.metadata.title" to false
            AudiobookshelfSort.AUTHOR -> "media.metadata.authorNameLF" to false
        }
        val body = get(
            "/api/libraries/${AudiobookshelfUrls.segment(libraryId)}/items",
            pageQuery(page) + mapOf("sort" to field, "desc" to if (isDescending) "1" else "0"),
        )
        return AudiobookshelfPage(AudiobookshelfJson.pageItems(body, ::coverUrlOf), hasMorePages(body, page, PAGE_SIZE))
    }

    fun search(libraryId: String, query: String): List<LibriVoxBook> {
        val body = get(
            "/api/libraries/${AudiobookshelfUrls.segment(libraryId)}/search",
            mapOf("q" to query, "limit" to SEARCH_ROWS.toString()),
        )
        return AudiobookshelfJson.searchItems(body, ::coverUrlOf)
    }

    fun series(libraryId: String, page: Int): AudiobookshelfPage<AudiobookshelfSeries> {
        val body = get(
            "/api/libraries/${AudiobookshelfUrls.segment(libraryId)}/series",
            pageQuery(page) + mapOf("sort" to "name", "desc" to "0"),
        )
        return AudiobookshelfPage(AudiobookshelfSeriesJson.series(body, ::coverUrlOf), hasMorePages(body, page, PAGE_SIZE))
    }

    fun seriesBooks(libraryId: String, seriesId: String): List<LibriVoxBook> {
        val body = get(
            "/api/libraries/${AudiobookshelfUrls.segment(libraryId)}/items",
            mapOf(
                "filter" to AudiobookshelfUrls.seriesFilter(seriesId),
                "limit" to SERIES_BOOK_ROWS.toString(),
                "page" to "0",
                "minified" to "1",
            ),
        )
        return AudiobookshelfSeriesJson.seriesBooks(body, seriesId, ::coverUrlOf)
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

    private fun pageQuery(page: Int) = mapOf("limit" to PAGE_SIZE.toString(), "page" to page.toString(), "minified" to "1")

    private fun get(path: String, query: Map<String, String> = emptyMap()): String = auth.authorized { serverUrl, access ->
        http.getText(serverUrl + path, query, access = access)
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_ROWS = 40
        const val SERIES_BOOK_ROWS = 200
        const val MAX_COVER_BYTES = 4 * 1024 * 1024
    }
}
