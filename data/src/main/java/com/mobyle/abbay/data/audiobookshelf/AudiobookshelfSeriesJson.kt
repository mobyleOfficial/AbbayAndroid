package com.mobyle.abbay.data.audiobookshelf

import com.model.AudiobookshelfSeries
import com.model.CatalogSeries
import com.model.LibriVoxBook
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Series in Audiobookshelf's answers. A full item names its series with ids and sequences; a minified one only as
 * "Name #2, Other #1", which still tells a book's place once its series is known.
 */
object AudiobookshelfSeriesJson {
    fun series(body: String, coverUrlOf: (String) -> String): List<AudiobookshelfSeries> {
        val results = objectOf(body)?.get("results") as? JsonArray ?: return emptyList()
        return results.mapNotNull { element ->
            val series = element as? JsonObject ?: return@mapNotNull null
            val id = series.string("id") ?: return@mapNotNull null
            val name = series.string("name") ?: return@mapNotNull null
            val books = (series["books"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            val first = books.minByOrNull { book -> sequenceIn(book, id, name)?.toDoubleOrNull() ?: Double.MAX_VALUE }
            AudiobookshelfSeries(
                id = id,
                name = name,
                bookCount = series.long("numBooks")?.toInt() ?: books.size,
                coverUrl = first?.string("id")?.let(coverUrlOf).orEmpty(),
            )
        }
    }

    /** The books of [seriesId] in a page of items, in their order in it. */
    fun seriesBooks(body: String, seriesId: String, coverUrlOf: (String) -> String): List<LibriVoxBook> {
        val results = (objectOf(body)?.get("results") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        // Minified items name their series without ids: the one they all share is this one.
        val name = results.firstNotNullOfOrNull { item -> listed(item).firstOrNull { it.string("id") == seriesId }?.string("name") }
            ?: results.map(::namesIn).reduceOrNull { shared, names -> shared intersect names }?.firstOrNull()
            ?: ""
        val books = results.mapNotNull { item ->
            AudiobookshelfJson.lightBook(item, coverUrlOf)?.copy(
                series = CatalogSeries(seriesId, name, sequenceIn(item, seriesId, name), libraryIdOf(item)),
            )
        }
        return books.sortedWith(compareBy<LibriVoxBook> { it.series?.order ?: Double.MAX_VALUE }.thenBy { it.title })
    }

    /** The first series an item's metadata names, with its id when the item is a full one. */
    fun seriesOf(item: JsonObject, metadata: JsonObject): CatalogSeries? {
        val entry = when (val series = metadata["series"]) {
            is JsonObject -> series
            is JsonArray -> series.firstOrNull() as? JsonObject
            else -> null
        }
        val full = entry?.string("name")?.let { name ->
            CatalogSeries(entry.string("id").orEmpty(), name, entry.string("sequence"), libraryIdOf(item))
        }
        return full ?: summaries(item).firstOrNull()?.let { (name, sequence) -> CatalogSeries("", name, sequence, libraryIdOf(item)) }
    }

    private fun sequenceIn(item: JsonObject, seriesId: String, seriesName: String): String? {
        listed(item).firstOrNull { it.string("id") == seriesId }?.let { return it.string("sequence") }
        return summaries(item).firstOrNull { (name, _) -> name == seriesName }?.second
    }

    private fun namesIn(item: JsonObject): Set<String> =
        (listed(item).mapNotNull { it.string("name") } + summaries(item).map { it.first }).toSet()

    /** The series a full item names, with ids. */
    private fun listed(
        item: JsonObject
    ): List<JsonObject> = when (val series = item.objectAt("media")?.objectAt("metadata")?.get("series")) {
        is JsonObject -> listOf(series)
        is JsonArray -> series.mapNotNull { it as? JsonObject }
        else -> emptyList()
    }

    /** The series a minified item names in its summary, "Name #2, Other #1". */
    private fun summaries(item: JsonObject): List<Pair<String, String?>> =
        item.objectAt("media")?.objectAt("metadata")?.string("seriesName")?.split(", ")?.map(::splitSeriesName).orEmpty()

    /** "Dune #2" to ("Dune", "2"); a name without a number has no sequence. */
    private fun splitSeriesName(text: String): Pair<String, String?> {
        val mark = text.lastIndexOf(" #")
        if (mark < 0) return text.trim() to null
        return text.substring(0, mark).trim() to text.substring(mark + 2).trim().takeIf(String::isNotEmpty)
    }

    private fun libraryIdOf(item: JsonObject) = item.string("libraryId").orEmpty()
}
