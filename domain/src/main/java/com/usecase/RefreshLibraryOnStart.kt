package com.usecase

import com.repository.LibraryRepository
import com.repository.SettingsRepository
import com.settings.RescanOnStart
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** App start: validates folder permissions, then scans as Settings → Library → "Rescan on start" says. */
class RefreshLibraryOnStart @Inject constructor(
    private val repository: LibraryRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke() {
        repository.validateRoots()
        when (settings.observeSettings().first().rescanOnStart) {
            RescanOnStart.OFF -> Unit
            RescanOnStart.QUICK -> repository.quickScan()
            RescanOnStart.FULL -> repository.requestFullScan()
        }
    }
}
