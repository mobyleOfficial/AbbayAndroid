package com.usecase

import com.model.Book
import com.readalong.ReadAlongAudioSource
import com.readalong.ReadAlongBookText
import com.readalong.ReadAlongEpub
import com.readalong.ReadAlongTranscript
import com.repository.ReadAlongRepository
import com.repository.SettingsRepository
import com.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetReadAlongEnabledTest {
    private class Settings : SettingsRepository {
        val flow = MutableStateFlow(AppSettings())

        override fun observeSettings() = flow

        override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
            flow.value = transform(flow.value)
        }
    }

    private class ReadAlong : ReadAlongRepository {
        var transcriptsDeleted = false
        var modelsDeleted = false

        override suspend fun load(bookId: String) = ReadAlongRepository.Loaded(null, 0)

        override suspend fun save(bookId: String, transcript: ReadAlongTranscript, stamp: Int) = Unit

        override suspend fun delete(bookId: String) = Unit

        override suspend fun deleteAll() {
            transcriptsDeleted = true
        }

        override suspend fun deleteModels() {
            modelsDeleted = true
        }

        override suspend fun language(bookId: String): String? = null

        override suspend fun setLanguage(bookId: String, locale: String?) = Unit

        override suspend fun syncOffset(bookId: String) = 0L

        override suspend fun setSyncOffset(bookId: String, offsetMs: Long) = Unit

        override suspend fun forgetBooks() = Unit

        override suspend fun audioSource(book: Book) = ReadAlongAudioSource(emptyList(), isDownloadable = false)

        override suspend fun bookText(book: Book): ReadAlongBookText? = null

        override suspend fun epub(book: Book): ReadAlongEpub? = null

        override suspend fun chooseEpub(bookId: String, uri: String?) = true

        val introSeen = mutableSetOf<String>()

        override suspend fun isIntroSeen(bookId: String) = bookId in introSeen

        override suspend fun setIntroSeen(bookId: String, seen: Boolean) {
            if (seen) introSeen += bookId else introSeen -= bookId
        }
    }

    private val settings = Settings()
    private val readAlong = ReadAlong()
    private val setEnabled = SetReadAlongEnabled(settings, readAlong)

    @Test
    fun `turning it on keeps what is on the device`() = runTest {
        setEnabled(true)

        assertTrue(settings.flow.value.readAlongEnabled)
        assertFalse(readAlong.transcriptsDeleted)
    }

    @Test
    fun `turning it off deletes every transcript and model`() = runTest {
        settings.flow.value = AppSettings(readAlongEnabled = true)

        setEnabled(false)

        assertFalse(settings.flow.value.readAlongEnabled)
        assertTrue(readAlong.transcriptsDeleted)
        assertTrue(readAlong.modelsDeleted)
    }
}
