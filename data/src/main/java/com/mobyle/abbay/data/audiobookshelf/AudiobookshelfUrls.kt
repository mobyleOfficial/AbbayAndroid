package com.mobyle.abbay.data.audiobookshelf

import java.net.URI
import java.net.URLEncoder
import java.util.Base64
import java.util.Locale

object AudiobookshelfUrls {
    /** Library books from Audiobookshelf share the LibriVox columns; this prefix tells their ids apart. */
    const val ID_PREFIX = "abs-"
    private const val COVER_WIDTH = 400

    fun libraryId(itemId: String) = ID_PREFIX + itemId

    fun itemIdOf(libraryId: String): String? = libraryId.takeIf { it.startsWith(ID_PREFIX) }?.removePrefix(ID_PREFIX)

    fun isAudiobookshelf(libraryId: String?) = libraryId?.startsWith(ID_PREFIX) == true

    fun fileUrl(serverUrl: String, itemId: String, ino: String) = "$serverUrl/api/items/${segment(itemId)}/file/${segment(ino)}"

    fun coverUrl(serverUrl: String, itemId: String) = "$serverUrl/api/items/${segment(itemId)}/cover?width=$COVER_WIDTH"

    /** The items filter for one series: the server takes its id in base64. */
    fun seriesFilter(seriesId: String) = "series." + Base64.getEncoder().encodeToString(seriesId.toByteArray(Charsets.UTF_8))

    fun itemPage(serverUrl: String, itemId: String) = "$serverUrl/item/${segment(itemId)}"

    /**
     * The server's base urls to try for what the user typed: with its scheme, or https then http (a home server often
     * has no certificate) when it has none. No trailing slash, no query. Empty when it is not a web address.
     */
    fun candidates(typed: String): List<String> {
        val text = typed.trim().trimEnd('/')
        if (text.isEmpty() || text.any(Char::isWhitespace)) return emptyList()
        val urls = if (text.contains("://")) listOf(text) else listOf("https://$text", "http://$text")
        return urls.mapNotNull(::normalized)
    }

    private fun normalized(text: String): String? {
        val uri = runCatching { URI(text) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)?.takeIf { it == "http" || it == "https" } ?: return null
        val host = uri.host?.takeIf(String::isNotEmpty) ?: return null
        val port = if (uri.port >= 0) ":${uri.port}" else ""
        val path = uri.rawPath.orEmpty().trimEnd('/')
        return "$scheme://$host$port$path"
    }

    /** [text] as one path segment. */
    fun segment(text: String) = URLEncoder.encode(text, Charsets.UTF_8.name()).replace("+", "%20")
}
