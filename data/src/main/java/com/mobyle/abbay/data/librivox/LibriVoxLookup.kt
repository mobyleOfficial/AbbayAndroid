package com.mobyle.abbay.data.librivox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface MappingLookup {
    data class Matched(val book: LibriVoxFeedBook) : MappingLookup

    data object NoMatch : MappingLookup

    data object Unavailable : MappingLookup
}

@Singleton
class LibriVoxLookup @Inject constructor(private val http: LibriVoxHttp, private val cache: CatalogCache) {
    suspend fun entry(identifier: String, creator: String?, recordsMiss: Boolean = true): MappingLookup {
        val known = cache.read(CachedMapping.serializer(), mappingKey(identifier), MISSED_MAPPING_MAX_AGE_MS)
        return known?.let { knownEntry(identifier, it) } ?: freshEntry(identifier, creator, recordsMiss)
    }

    fun recordMiss(identifier: String) = cache.write(CachedMapping.serializer(), mappingKey(identifier), CachedMapping(null))

    private suspend fun knownEntry(identifier: String, known: CatalogCache.Hit<CachedMapping>): MappingLookup? {
        val knownId = known.value.librivoxId ?: return MappingLookup.NoMatch.takeIf { known.isFresh }
        val books = feedBooks(mapOf("id" to knownId)) ?: return MappingLookup.Unavailable
        return match(books, identifier)?.let { MappingLookup.Matched(it) }.also { if (it == null) cache.remove(mappingKey(identifier)) }
    }

    private suspend fun freshEntry(identifier: String, creator: String?, recordsMiss: Boolean): MappingLookup {
        val isVarious = creator?.trim()?.equals("various", ignoreCase = true) == true
        val lastName = CatalogText.lastName(creator)
        val lookup = if (!isVarious && lastName != null) lookUpByAuthor(identifier, lastName) else MappingLookup.NoMatch
        when {
            lookup is MappingLookup.Matched -> cache.write(
                CachedMapping.serializer(),
                mappingKey(identifier),
                CachedMapping(lookup.book.id)
            )
            lookup == MappingLookup.NoMatch && recordsMiss -> recordMiss(identifier)
        }
        return lookup
    }

    private suspend fun lookUpByAuthor(identifier: String, lastName: String): MappingLookup = coroutineScope {
        val first = authorPage(lastName, page = 0) ?: return@coroutineScope MappingLookup.Unavailable
        match(first, identifier)?.let { return@coroutineScope MappingLookup.Matched(it) }
        if (first.size < AUTHOR_PAGE_SIZE) return@coroutineScope MappingLookup.NoMatch
        val rest = (1 until AUTHOR_PAGE_CAP).map { page -> async { authorPage(lastName, page) } }.awaitAll()
        match(rest.filterNotNull().flatten(), identifier)?.let { MappingLookup.Matched(it) }
            ?: if (rest.any { it == null }) MappingLookup.Unavailable else MappingLookup.NoMatch
    }

    private suspend fun authorPage(lastName: String, page: Int) = feedBooks(
        mapOf("author" to lastName, "limit" to AUTHOR_PAGE_SIZE.toString(), "offset" to (page * AUTHOR_PAGE_SIZE).toString()),
    )

    @Suppress("SwallowedException")
    private suspend fun feedBooks(extra: Map<String, String>): List<LibriVoxFeedBook>? = withContext(Dispatchers.IO) {
        val query = mapOf("format" to "json", "extended" to "1", "fields" to FEED_FIELDS) + extra
        try {
            LibriVoxJson.feedBooks(http.getText(ArchiveCatalog.FEED_ENDPOINT, query, FEED_TIMEOUT_MS))
        } catch (notFound: HttpStatusException) {
            if (notFound.status == HTTP_NOT_FOUND) emptyList() else null
        } catch (unreachable: IOException) {
            null
        }
    }

    private fun match(books: List<LibriVoxFeedBook>, identifier: String) =
        books.firstOrNull { it.archiveIdentifier == identifier && it.id != null }

    private companion object {
        const val FEED_FIELDS = "{id,url_iarchive,sections,description}"
        const val AUTHOR_PAGE_SIZE = 100
        const val AUTHOR_PAGE_CAP = 3
        const val FEED_TIMEOUT_MS = 20_000
        const val HTTP_NOT_FOUND = 404
        val MISSED_MAPPING_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)

        fun mappingKey(identifier: String) = "mapping.$identifier"
    }
}
