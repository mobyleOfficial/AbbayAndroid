package com.mobyle.abbay.data.audiobookshelf

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

private val json = Json { ignoreUnknownKeys = true }
private const val MILLIS_PER_SECOND = 1_000.0

internal fun parseJson(body: String): JsonElement? = runCatching { json.parseToJsonElement(body) }.getOrNull()

internal fun objectOf(body: String): JsonObject? = parseJson(body) as? JsonObject

internal fun JsonObject.objectAt(key: String): JsonObject? = get(key) as? JsonObject

internal fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)

internal fun JsonObject.long(key: String): Long? = (get(key) as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

internal fun JsonObject.boolean(key: String): Boolean? = (get(key) as? JsonPrimitive)?.booleanOrNull

/** Audiobookshelf counts time in seconds. */
internal fun JsonObject.millis(key: String): Long? =
    (get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf { it >= 0 }?.let { (it * MILLIS_PER_SECOND).toLong() }

/**
 * Whether a page of [limit] rows, number [page] from 0, has more after it: by the total the server gives, or, from a
 * server that gives none, by a full page.
 */
internal fun hasMorePages(body: String, page: Int, limit: Int): Boolean {
    val root = objectOf(body) ?: return false
    val rows = (root["results"] as? JsonArray)?.size ?: 0
    val total = root.long("total")
    return if (total != null) (page + 1).toLong() * limit < total else rows >= limit
}

/** Names from a list of people, as objects with a name or as plain strings. */
internal fun names(element: JsonElement?): String = (element as? JsonArray).orEmpty().mapNotNull { person ->
    when (person) {
        is JsonObject -> person.string("name")
        is JsonPrimitive -> person.contentOrNull
        else -> null
    }
}.joinToString(", ")
