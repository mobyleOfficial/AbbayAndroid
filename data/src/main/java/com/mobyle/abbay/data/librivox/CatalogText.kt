package com.mobyle.abbay.data.librivox

import java.util.Locale

object CatalogText {
    private val nameSuffixes = setOf("jr", "jr.", "sr", "sr.", "ii", "iii", "iv")
    private val whitespace = Regex("\\s+")
    private val lineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val tag = Regex("<[^>]+>")
    private val entities = listOf("&quot;" to "\"", "&#39;" to "'", "&apos;" to "'", "&lt;" to "<", "&gt;" to ">", "&nbsp;" to " ")

    fun cleanTitle(title: String, creator: String?): String {
        val trimmed = title.trim()
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

    fun stripHtml(html: String): String {
        val text = entities.fold(html.replace(lineBreak, "\n").replace(tag, "")) { text, (entity, replacement) ->
            text.replace(entity, replacement)
        }
        return text.replace("&amp;", "&").trim()
    }

    private fun isNameSuffix(word: String) = word.lowercase(Locale.ROOT) in nameSuffixes
}
