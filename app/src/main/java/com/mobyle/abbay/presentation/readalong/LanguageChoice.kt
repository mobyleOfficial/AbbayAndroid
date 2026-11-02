package com.mobyle.abbay.presentation.readalong

import com.readalong.ReadAlongLanguage
import java.text.Collator
import java.util.Locale

/** One language-menu entry: the locale id and what the menu calls it. */
data class LanguageChoice(val id: String, val name: String)

/**
 * The menu's entries in alphabetical order. A choice is named by its language alone ("English"), unless another choice
 * shares it: both then carry their region ("English (United States)").
 */
fun languageChoices(ids: List<String>, locale: Locale): List<LanguageChoice> {
    val languages = ids.map { ReadAlongLanguage.displayName(it, locale) }
    val collator = Collator.getInstance(locale)
    return ids.mapIndexed { index, id ->
        val isShared = languages.count { it == languages[index] } > 1
        LanguageChoice(id, if (isShared) ReadAlongLanguage.regionalName(id, locale) else languages[index])
    }.sortedWith { first, second -> collator.compare(first.name, second.name) }
}
