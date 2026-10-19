package com.usecase

import com.repository.LanguageRepository
import javax.inject.Inject

class GetAppLanguage @Inject constructor(private val repository: LanguageRepository) {
    operator fun invoke() = repository.currentLanguage()
}
