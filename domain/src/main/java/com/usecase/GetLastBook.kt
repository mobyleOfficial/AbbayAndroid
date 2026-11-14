package com.usecase

import com.model.Book
import com.repository.LibraryRepository
import com.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetLastBook @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(): Book? {
        val lastBookId = settingsRepository.observeSettings().first().lastBookId ?: return null
        return libraryRepository.observeBooks().first().firstOrNull { it.id == lastBookId && !it.hasError }
    }
}
