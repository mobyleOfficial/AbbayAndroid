package com.usecase

import com.repository.LanguageRepository
import com.settings.AppLanguage
import javax.inject.Inject

class SetAppLanguage @Inject constructor(private val repository: LanguageRepository) {
    operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}
