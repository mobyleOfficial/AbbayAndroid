package com.usecase

import com.model.DataError
import com.model.DataResult
import com.repository.LibraryRepository
import com.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * An image shared to Abbay after "Search the web" becomes the cover of the book that asked for it
 *.
 */
class ApplySharedCover @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(imageUri: String): DataResult<Unit> {
        val bookId = settingsRepository.observeSettings().first().pendingCoverBookId
            ?: return DataResult.Failure(DataError.UNREADABLE)
        val result = libraryRepository.setUserCover(bookId, imageUri)
        if (result is DataResult.Success) settingsRepository.updateSettings { it.copy(pendingCoverBookId = null) }
        return result
    }
}
