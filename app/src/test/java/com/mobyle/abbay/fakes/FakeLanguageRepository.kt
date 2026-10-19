package com.mobyle.abbay.fakes

import com.repository.LanguageRepository
import com.settings.AppLanguage

class FakeLanguageRepository(var storedLanguage: AppLanguage = AppLanguage.SYSTEM) : LanguageRepository {
    override fun currentLanguage(): AppLanguage = storedLanguage

    override fun setLanguage(language: AppLanguage) {
        storedLanguage = language
    }
}
