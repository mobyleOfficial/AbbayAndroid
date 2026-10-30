package com.mobyle.abbay.data.librivox

import com.model.DataError
import com.model.LibriVoxBook
import com.model.LibriVoxDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class LibriVoxException(val error: DataError) : Exception(error.name)

@Singleton
class LibriVoxCatalog @Inject constructor(
    private val http: LibriVoxHttp,
    private val cache: CatalogCache,
    private val lookup: LibriVoxLookup,
) {
    private val sessionSearches = LinkedHashMap<String, List<LibriVoxBook>>()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var popularRefresh: Job? = null
    private val popularSerializer = ListSerializer(CachedBook.serializer())

    suspend fun search(text: String): List<LibriVoxBook> {
        val lucene = ArchiveSearchQuery.search(text) ?: return emptyList()
        val key = ArchiveSearchQuery.normalized(text)
        return synchronized(sessionSearches) { sessionSearches[key] } ?: searchArchive(lucene, SEARCH_ROWS).also { books ->
            synchronized(sessionSearches) {
                if (sessionSearches.size >= SESSION_SEARCH_LIMIT) sessionSearches.clear()
                sessionSearches[key] = books
            }
        }
    }

    suspend fun popular(): List<LibriVoxBook> {
        val cached = cache.read(popularSerializer, POPULAR_KEY, POPULAR_MAX_AGE_MS) ?: return fetchPopular()
        if (!cached.isFresh) refreshPopularInBackground()
        return cached.value.map(CachedBook::toDomain)
    }

    fun details(book: LibriVoxBook): Flow<LibriVoxDetails> = flow {
        val cached = cache.read(CachedBook.serializer(), detailsKey(book.id), DETAILS_MAX_AGE_MS)
        if (cached?.isFresh == true) {
            emit(LibriVoxDetails(cached.value.toDomain(), isAwaitingTitles = false))
        } else {
            fetchDetails(book, stale = cached?.value?.toDomain())
        }
    }.flowOn(Dispatchers.IO)

    fun coverBytes(book: LibriVoxBook): ByteArray? = runCatching { http.getBytes(book.coverUrl, MAX_COVER_BYTES) }.getOrNull()

    private suspend fun FlowCollector<LibriVoxDetails>.fetchDetails(book: LibriVoxBook, stale: LibriVoxBook?) = coroutineScope {
        val early = book.creator?.let { creator -> async { lookup.entry(book.id, creator, recordsMiss = false) } }
        val item = try {
            fetchArchiveItem(book.id)
        } catch (unreachable: IOException) {
            early?.cancel()
            if (stale == null) throw unreachable
            emit(LibriVoxDetails(stale, isAwaitingTitles = false))
            return@coroutineScope
        }
        (stale ?: ArchiveCatalog.book(item, match = null))?.let { preview -> emit(LibriVoxDetails(preview, isAwaitingTitles = true)) }
        val mapping = resolveMapping(early, book, item)
        val fetched = ArchiveCatalog.book(item, (mapping as? MappingLookup.Matched)?.book)
            ?: throw LibriVoxException(DataError.NOT_AN_AUDIOBOOK)
        if (mapping == MappingLookup.Unavailable) {
            emit(LibriVoxDetails(stale ?: fetched, isAwaitingTitles = false))
        } else {
            cache.write(CachedBook.serializer(), detailsKey(book.id), CachedBook.of(fetched))
            emit(LibriVoxDetails(fetched, isAwaitingTitles = false))
        }
    }

    private suspend fun resolveMapping(early: Deferred<MappingLookup>?, book: LibriVoxBook, item: ArchiveItem): MappingLookup {
        if (early == null || item.creator != book.creator) {
            early?.cancel()
            return lookup.entry(book.id, item.creator)
        }
        return early.await().also { if (it == MappingLookup.NoMatch) lookup.recordMiss(book.id) }
    }

    private fun refreshPopularInBackground() = synchronized(this) {
        if (popularRefresh?.isActive != true) popularRefresh = backgroundScope.launch { runCatching { fetchPopular() } }
    }

    private suspend fun fetchPopular(): List<LibriVoxBook> {
        val books = searchArchive(ArchiveSearchQuery.CATALOG, POPULAR_ROWS)
        if (books.isNotEmpty()) cache.write(popularSerializer, POPULAR_KEY, books.map(CachedBook::of))
        return books
    }

    private suspend fun searchArchive(lucene: String, rows: Int): List<LibriVoxBook> = withContext(Dispatchers.IO) {
        val body = http.getText(
            ArchiveCatalog.SEARCH_ENDPOINT,
            mapOf(
                "q" to lucene,
                "fl[]" to SEARCH_FIELDS,
                "sort[]" to "downloads desc",
                "rows" to rows.toString(),
                "page" to "1",
                "output" to "json",
            ),
        )
        LibriVoxJson.searchDocs(body).mapNotNull(ArchiveCatalog::lightBook).distinctBy { it.id }
    }

    private suspend fun fetchArchiveItem(identifier: String): ArchiveItem = withContext(Dispatchers.IO) {
        val item = LibriVoxJson.archiveItem(identifier, http.getText(ArchiveCatalog.METADATA_ENDPOINT + identifier))
        item?.takeIf { it.mediatype == null || it.mediatype == "audio" } ?: throw LibriVoxException(DataError.NOT_AN_AUDIOBOOK)
    }

    private companion object {
        const val POPULAR_KEY = "popular"
        const val SEARCH_FIELDS = "identifier,title,creator,language,mediatype"
        const val SEARCH_ROWS = 50
        const val POPULAR_ROWS = 30
        const val SESSION_SEARCH_LIMIT = 50
        const val MAX_COVER_BYTES = 4 * 1024 * 1024
        val POPULAR_MAX_AGE_MS = TimeUnit.DAYS.toMillis(1)
        val DETAILS_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)

        fun detailsKey(identifier: String) = "details.$identifier"
    }
}
