package com.repository

import com.settings.AppLanguage

interface LanguageRepository {
    fun currentLanguage(): AppLanguage

    fun setLanguage(language: AppLanguage)
}
