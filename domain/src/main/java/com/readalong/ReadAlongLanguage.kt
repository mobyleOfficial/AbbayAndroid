package com.readalong

import java.util.Locale

/**
 * Pure helpers that turn LibriVox's language labels into codes, list the languages worth trying when detecting what a
 * book is read in, pick the most confident one, and match a language to a supported locale. Locale ids are BCP-47 with
 * a hyphen ("pt-BR"); inputs with an underscore are normalised first.
 */
object ReadAlongLanguage {
    /** LibriVox gives "eng", "English", "por", "Portuguese"…: a two-letter language code ("en"), or null. */
    fun languageCode(fromLibriVox: String): String? {
        val label = fromLibriVox.trim().lowercase(Locale.ROOT)
        val isCode = label.isNotEmpty() && label.all { it in 'a'..'z' }
        return BIBLIOGRAPHIC_CODES[label]
            ?: namesToCodes[label]
            ?: when {
                isCode && label.length == ALPHA2_LENGTH -> label.takeIf { it in Locale.getISOLanguages() }
                isCode && label.length == ALPHA3_LENGTH -> threeLetterToTwo[label]
                else -> null
            }
    }

    /**
     * Candidate locales for detection, in order, deduplicated, only among [installed]: the app language, then the
     * device's [preferred] ones, then English. A bare app language ("pt") takes the region of the first preferred entry
     * in that language.
     */
    fun candidates(appLanguage: String?, preferred: List<String>, installed: List<String>): List<String> {
        val normalisedInstalled = installed.map(::normalised)
        val normalisedPreferred = preferred.map(::normalised)
        val result = LinkedHashSet<String>()
        if (appLanguage != null) {
            val code = parts(appLanguage).language
            val region = normalisedPreferred.map(::parts).firstOrNull { it.language == code && it.region != null }?.region
                ?: parts(appLanguage).region
            locale(code, normalisedInstalled, preferredRegion = region)?.let(result::add)
        }
        for (id in normalisedPreferred) {
            val exact = normalisedInstalled.firstOrNull { it.equals(id, ignoreCase = true) }
            (exact ?: locale(parts(id).language, normalisedInstalled, preferredRegion = null))?.let(result::add)
        }
        locale("en", normalisedInstalled, preferredRegion = null)?.let(result::add)
        return result.toList()
    }

    /**
     * The supported locale for a language code ("pt" → "pt-BR"), in order: the one in [preferredRegion]; an installed
     * one; the language's likely region; the first of that language.
     */
    fun locale(code: String, supported: List<String>, installed: List<String> = emptyList(), preferredRegion: String?): String? {
        val language = code.lowercase(Locale.ROOT)
        val matches = supported.map(::normalised).filter { parts(it).language == language }
        val installedSet = installed.map { normalised(it).lowercase(Locale.ROOT) }.toSet()
        val installedMatches = matches.filter { it.lowercase(Locale.ROOT) in installedSet }
        val likely = LIKELY_REGIONS[language]
        fun inRegion(region: String?, among: List<String>) =
            region?.let { wanted -> among.firstOrNull { parts(it).region.equals(wanted, ignoreCase = true) } }
        return inRegion(preferredRegion, matches)
            ?: inRegion(likely, installedMatches)
            ?: installedMatches.firstOrNull()
            ?: inRegion(likely, matches)
            ?: matches.firstOrNull()
    }

    /** The locale with the best mean confidence if it reaches [minimumConfidence]; ties go to the first id alphabetically. */
    fun best(scores: Map<String, Double>, minimumConfidence: Double = MINIMUM_CONFIDENCE): String? = scores.entries
        .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.takeIf { it.value >= minimumConfidence }
        ?.key

    /** "English", "Português": the language alone, in [inLocale], capitalised. */
    fun displayName(localeId: String, inLocale: Locale): String {
        val code = parts(localeId).language
        val name = Locale(code).getDisplayLanguage(inLocale).takeIf { it.isNotBlank() && it != code } ?: return localeId
        return name.replaceFirstChar { it.titlecase(inLocale) }
    }

    /** "English (United States)": with the region, for when two choices share a language. */
    fun regionalName(localeId: String, inLocale: Locale): String =
        Locale.forLanguageTag(normalised(localeId)).getDisplayName(inLocale).replaceFirstChar { it.titlecase(inLocale) }

    fun languageOf(localeId: String): String = parts(localeId).language

    fun isBareCode(id: String): Boolean = '-' !in id && '_' !in id

    private fun normalised(id: String): String = id.replace('_', '-')

    private data class Parts(val language: String, val region: String?)

    private fun parts(id: String): Parts {
        val components = normalised(id).split('-')
        val region = components.drop(1).firstOrNull { part ->
            (part.length == ALPHA2_LENGTH && part.all(Char::isLetter)) || (part.length == ALPHA3_LENGTH && part.all(Char::isDigit))
        }
        return Parts(components.first().lowercase(Locale.ROOT), region)
    }

    private const val MINIMUM_CONFIDENCE = 0.5
    private const val ALPHA2_LENGTH = 2
    private const val ALPHA3_LENGTH = 3

    /** The ISO 639-2/B codes LibriVox uses that differ from the terminological ones Java knows ("ger" rather than "deu"). */
    private val BIBLIOGRAPHIC_CODES = mapOf(
        "alb" to "sq", "arm" to "hy", "baq" to "eu", "bur" to "my", "chi" to "zh", "cze" to "cs", "dut" to "nl",
        "fre" to "fr", "geo" to "ka", "ger" to "de", "gre" to "el", "ice" to "is", "mac" to "mk", "mao" to "mi",
        "may" to "ms", "per" to "fa", "rum" to "ro", "slo" to "sk", "tib" to "bo", "wel" to "cy",
    )

    private val threeLetterToTwo: Map<String, String> by lazy {
        Locale.getISOLanguages().mapNotNull { code ->
            runCatching { Locale(code).isO3Language }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { it to code }
        }.toMap()
    }

    private val namesToCodes: Map<String, String> by lazy {
        Locale.getISOLanguages().associateBy { Locale(it).getDisplayLanguage(Locale.ENGLISH).lowercase(Locale.ROOT) }
    }

    /** The region each supported language is most likely spoken in, for a bare code. */
    private val LIKELY_REGIONS = mapOf(
        "en" to "US", "pt" to "BR", "es" to "ES", "fr" to "FR", "de" to "DE", "it" to "IT", "nl" to "NL", "ru" to "RU",
        "pl" to "PL", "ca" to "ES", "tr" to "TR", "cs" to "CZ",
    )
}
