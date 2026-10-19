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

/**
 * Per-app language. Android 13+: the platform's `LocaleManager` keeps it and applies it
 * everywhere. Android 10–12 (the reference device): the tag is kept in private preferences and applied by [wrap]
 * (activities) and [applyToApplication] (the application's resources, which services and widgets share).
 */
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

        /** Android 10–12: [base] with the chosen language, for `attachBaseContext`; unchanged otherwise. */
        fun wrap(base: Context): Context {
            val locale = chosenLocale(base) ?: return base
            val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(configuration)
        }

        /** Android 10–12: makes the application's shared resources use the chosen (or again the system) language. */
        @Suppress("DEPRECATION") // The shared application resources have no non-deprecated way to change locale.
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
