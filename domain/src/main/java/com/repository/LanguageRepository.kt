package com.repository

import com.settings.AppLanguage

/** The per-app language, kept by the platform where it can (Android 13+), else by the app. */
interface LanguageRepository {
    fun currentLanguage(): AppLanguage

    fun setLanguage(language: AppLanguage)
}
