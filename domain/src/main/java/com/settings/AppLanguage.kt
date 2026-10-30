package com.settings

enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    PORTUGUESE("pt-BR"),
    SPANISH("es"),
    ;

    companion object {
        fun ofTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag.isNotEmpty() && it.tag.equals(tag, ignoreCase = true) }
            ?: entries.firstOrNull {
                it.tag.isNotEmpty() && tag?.substringBefore('-').equals(it.tag.substringBefore('-'), ignoreCase = true)
            }
            ?: SYSTEM
    }
}
