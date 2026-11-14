package com.mobyle.abbay.infra.language

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.repository.LanguageRepository
import com.settings.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class AppLanguageStore @Inject constructor(@ApplicationContext private val context: Context) : LanguageRepository {
    override fun currentLanguage(): AppLanguage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AppLanguage.ofTag(context.getSystemService(LocaleManager::class.java).applicationLocales.get(0)?.toLanguageTag())
    } else {
        AppLanguage.ofTag(storedTag(context))
    }

    override fun setLanguage(language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language.tag)
        } else {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY_TAG, language.tag).apply()
            applyToApplication(context)
        }
    }

    companion object {
        private const val PREFERENCES = "app_language"
        private const val KEY_TAG = "tag"

        fun wrap(base: Context): Context {
            val locale = chosenLocale(base) ?: return base
            val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(configuration)
        }

        @Suppress("DEPRECATION")
        fun applyToApplication(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            val locale = chosenLocale(context) ?: Configuration(android.content.res.Resources.getSystem().configuration).locales[0]
            Locale.setDefault(locale)
            val resources = context.applicationContext.resources
            val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        private fun chosenLocale(context: Context): Locale? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return null
            return storedTag(context)?.takeIf { it.isNotEmpty() }?.let(Locale::forLanguageTag)
        }

        private fun storedTag(context: Context): String? =
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY_TAG, null)
    }
}
