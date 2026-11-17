package com.mobyle.abbay.data.librivox

import java.text.Normalizer
import java.util.Locale

object CatalogText {
    private val nameSuffixes = setOf("jr", "jr.", "sr", "sr.", "ii", "iii", "iv")
    private val whitespace = Regex("\\s+")
    private val lineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val tag = Regex("<[^>]+>")
    private val entity = Regex("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[A-Za-z][A-Za-z0-9]{1,9});")
    private const val HEX_RADIX = 16
    private val namedEntities = mapOf(
        "amp" to "&", "quot" to "\"", "apos" to "'", "lt" to "<", "gt" to ">", "nbsp" to " ",
        "szlig" to "ß", "aelig" to "æ", "AElig" to "Æ", "oelig" to "œ", "OElig" to "Œ", "oslash" to "ø", "Oslash" to "Ø",
        "eth" to "ð", "ETH" to "Ð", "thorn" to "þ", "THORN" to "Þ", "iexcl" to "¡", "iquest" to "¿",
        "laquo" to "«", "raquo" to "»", "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "middot" to "·", "deg" to "°", "copy" to "©", "reg" to "®",
        "ordf" to "ª", "ordm" to "º", "euro" to "€",
    )
    private val accentMarks = mapOf(
        "acute" to "\u0301",
        "grave" to "\u0300",
        "circ" to "\u0302",
        "tilde" to "\u0303",
        "uml" to "\u0308",
        "ring" to "\u030A",
        "cedil" to "\u0327",
        "caron" to "\u030C",
    )

    fun cleanTitle(title: String, creator: String?): String {
        val trimmed = decodeEntities(title).trim()
        val suffix = creator?.takeIf(String::isNotEmpty)?.let { ", by $it" }
        return if (suffix != null && trimmed.length > suffix.length && trimmed.endsWith(suffix, ignoreCase = true)) {
            trimmed.dropLast(suffix.length).trim()
        } else {
            trimmed
        }
    }

    fun lastName(creator: String?): String? {
        val parts = creator.orEmpty().split(',').map(String::trim).filter { it.isNotEmpty() && !isNameSuffix(it) }
        val words = parts.firstOrNull()?.split(whitespace)?.filter(String::isNotEmpty) ?: return null
        return if (parts.size > 1) words.joinToString(" ") else lastWordBeforeSuffixes(words)
    }

    private fun lastWordBeforeSuffixes(words: List<String>): String? {
        val kept = words.toMutableList()
        while (kept.size > 1 && isNameSuffix(kept.last())) kept.removeAt(kept.lastIndex)
        return kept.lastOrNull()
    }

    fun stripHtml(html: String): String = decodeEntities(html.replace(lineBreak, "\n").replace(tag, "")).trim()

    /**
     * LibriVox and archive.org send some titles HTML-escaped ("El celoso extreme&ntilde;o"). Decodes named entities
     * (the common ones and every Latin accented letter) and numeric ones in a single pass, so "&amp;lt;" stays "&lt;".
     * Anything unknown is left as it was.
     */
    fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        return entity.replace(text) { match ->
            val name = match.groupValues[1]
            val decoded = when {
                name.startsWith("#x", ignoreCase = true) -> name.drop(2).toIntOrNull(HEX_RADIX)?.let(::codePoint)
                name.startsWith("#") -> name.drop(1).toIntOrNull()?.let(::codePoint)
                else -> namedEntities[name] ?: accentedLetter(name)
            }
            decoded ?: match.value
        }
    }

    private fun codePoint(value: Int): String? = value.takeIf(Character::isValidCodePoint)?.let { String(Character.toChars(it)) }

    private fun accentedLetter(name: String): String? {
        if (name.length < 2) return null
        val mark = accentMarks[name.drop(1)] ?: return null
        val composed = Normalizer.normalize("${name[0]}$mark", Normalizer.Form.NFC)
        return composed.takeIf { it.length == 1 }
    }

    private fun isNameSuffix(word: String) = word.lowercase(Locale.ROOT) in nameSuffixes
}
