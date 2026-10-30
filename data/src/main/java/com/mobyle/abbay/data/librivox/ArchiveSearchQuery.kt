package com.mobyle.abbay.data.librivox

import java.util.Locale

object ArchiveSearchQuery {
    const val CATALOG = "collection:librivoxaudio AND mediatype:audio"

    private const val SPECIAL_CHARACTERS = "-&|!(){}[]^\"~*?:\\/"

    fun search(text: String): String? {
        val terms = terms(text)
        if (terms.isEmpty()) return null
        return "$CATALOG AND " + terms.joinToString(" AND ") { term -> "(title:$term* OR creator:$term*)" }
    }

    fun normalized(text: String): String = terms(text).joinToString(" ")

    fun terms(text: String): List<String> = text.lowercase(Locale.ROOT)
        .replace('+', ' ')
        .split(WHITESPACE)
        .filter(String::isNotEmpty)
        .map { word ->
            buildString {
                word.forEach { character ->
                    if (character in SPECIAL_CHARACTERS) append('\\')
                    append(character)
                }
            }
        }

    private val WHITESPACE = Regex("\\s+")
}
