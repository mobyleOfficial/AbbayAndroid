package com.mobyle.abbay.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.model.BackupSummary
import com.model.DataResult
import com.model.LibraryRoot
import com.settings.AppLanguage
import com.settings.AppSettings
import com.settings.ThemeMode
import com.usecase.AddLibraryRoot
import com.usecase.ClearLibrary
import com.usecase.ClearStatistics
import com.usecase.ExportBackup
import com.usecase.GetAppLanguage
import com.usecase.ImportBackup
import com.usecase.ObserveAppSettings
import com.usecase.ObserveLibraryRoots
import com.usecase.RemoveLibraryRoot
import com.usecase.ReplaceLibraryRoots
import com.usecase.SetAppLanguage
import com.usecase.UpdateAppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val updateAppSettings: UpdateAppSettings,
    private val replaceLibraryRoots: ReplaceLibraryRoots,
    private val addLibraryRoot: AddLibraryRoot,
    private val removeLibraryRoot: RemoveLibraryRoot,
    private val clearLibrary: ClearLibrary,
    private val exportBackup: ExportBackup,
    private val importBackup: ImportBackup,
    private val clearStatistics: ClearStatistics,
    observeAppSettings: ObserveAppSettings,
    observeLibraryRoots: ObserveLibraryRoots,
    getAppLanguage: GetAppLanguage,
    private val setAppLanguage: SetAppLanguage,
) : ViewModel() {
    private val settings: StateFlow<AppSettings> = observeAppSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val _showShowDeleteConfirmation = MutableStateFlow(false)
    private val _showChangeFolderConfirmation = MutableStateFlow(false)
    private val _showDisableAdsConfirmation = MutableStateFlow(false)
    private val _backupResults = MutableSharedFlow<BackupResult>(extraBufferCapacity = 1)
    private val _language = MutableStateFlow(getAppLanguage())

    /** Settings → Appearance → Language. */
    val language: StateFlow<AppLanguage> get() = _language

    /** Outcome of the last export or import, shown once by the screen. */
    val backupResults: SharedFlow<BackupResult> get() = _backupResults.asSharedFlow()

    val shouldPlayWhenAppIsClosed: StateFlow<Boolean> = settings.mapState { it.playWhenAppIsClosed }
    val shouldOpenPlayerInStartup: StateFlow<Boolean> = settings.mapState { it.openPlayerOnStartup }
    val themeMode: StateFlow<ThemeMode> = settings.mapState { it.themeMode }
    val dynamicColor: StateFlow<Boolean> = settings.mapState { it.dynamicColor }
    val shareDiagnostics: StateFlow<Boolean> = settings.mapState { it.shareDiagnostics }
    val showAds: StateFlow<Boolean> = settings.mapState { it.showAds }

    /** Turning ads off asks first: they keep the app going, and the change applies on the next launch. */
    val showDisableAdsConfirmation: StateFlow<Boolean> get() = _showDisableAdsConfirmation

    /** Playback and sleep timer settings. */
    val listeningSettings: StateFlow<AppSettings> get() = settings
    val showShowDeleteConfirmation: StateFlow<Boolean> get() = _showShowDeleteConfirmation
    val showChangeFolderConfirmation: StateFlow<Boolean> get() = _showChangeFolderConfirmation
    val libraryRoots: StateFlow<List<LibraryRoot>> = observeLibraryRoots()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun changePlayWhenAppIsClosed(shouldPlayWhenAppIsClosed: Boolean) = viewModelScope.launch {
        updateAppSettings { it.copy(playWhenAppIsClosed = shouldPlayWhenAppIsClosed) }
    }

    fun changeOpenPlayerInStartUp(shouldOpenPlayerInStartUp: Boolean) = viewModelScope.launch {
        updateAppSettings { it.copy(openPlayerOnStartup = shouldOpenPlayerInStartUp) }
    }

    fun changeThemeMode(themeMode: ThemeMode) = viewModelScope.launch { updateAppSettings { it.copy(themeMode = themeMode) } }

    fun changeDynamicColor(isEnabled: Boolean) = viewModelScope.launch { updateAppSettings { it.copy(dynamicColor = isEnabled) } }

    fun changeLanguage(language: AppLanguage) {
        setAppLanguage(language)
        _language.value = language
        viewModelScope.launch { updateAppSettings { it.copy(language = language) } }
    }

    fun changeShareDiagnostics(isEnabled: Boolean) = viewModelScope.launch { updateAppSettings { it.copy(shareDiagnostics = isEnabled) } }

    fun changeShowAds(isEnabled: Boolean) {
        if (isEnabled) {
            viewModelScope.launch { updateAppSettings { it.copy(showAds = true) } }
        } else {
            _showDisableAdsConfirmation.value = true
        }
    }

    fun confirmDisableAds() {
        _showDisableAdsConfirmation.value = false
        viewModelScope.launch { updateAppSettings { it.copy(showAds = false) } }
    }

    fun dismissDisableAdsConfirmation() {
        _showDisableAdsConfirmation.value = false
    }

    /** Every playback and sleep timer setting is a field of [AppSettings]; the screen passes the change. */
    fun changeListeningSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateAppSettings(transform) }
    }

    fun exportBackupTo(documentUri: String) = viewModelScope.launch {
        _backupResults.tryEmit(BackupResult.of(isExport = true, result = exportBackup(documentUri)))
    }

    fun importBackupFrom(documentUri: String) = viewModelScope.launch {
        _backupResults.tryEmit(BackupResult.of(isExport = false, result = importBackup(documentUri)))
    }

    /** "Change folder" replaces the library root. */
    fun changeFolder(treeUri: String) = viewModelScope.launch { replaceLibraryRoots(treeUri) }

    fun addFolder(treeUri: String) = viewModelScope.launch { addLibraryRoot(treeUri) }

    fun removeFolder(rootId: Long) = viewModelScope.launch { removeLibraryRoot(rootId) }

    fun clearBooks() = viewModelScope.launch { clearLibrary() }

    /** "Clear statistics"; turning them off keeps the data. */
    fun clearListeningStatistics() = viewModelScope.launch { clearStatistics() }

    fun dismissDeleteConfirmation() {
        _showShowDeleteConfirmation.value = false
    }

    fun showDeleteConfirmation() {
        _showShowDeleteConfirmation.value = true
    }

    fun dismissChangeFolderConfirmation() {
        _showChangeFolderConfirmation.value = false
    }

    fun showChangeFolderConfirmation() {
        _showChangeFolderConfirmation.value = true
    }

    private fun <T> StateFlow<AppSettings>.mapState(transform: (AppSettings) -> T): StateFlow<T> =
        map(transform).stateIn(viewModelScope, SharingStarted.Eagerly, transform(value))
}

/** What the screen says after a backup export or import. */
sealed interface BackupResult {
    data class Exported(val books: Int) : BackupResult

    data class Imported(val books: Int, val unmatchedBooks: Int) : BackupResult

    data object NotABackup : BackupResult

    data object Failed : BackupResult

    companion object {
        fun of(isExport: Boolean, result: DataResult<BackupSummary>): BackupResult = when (result) {
            is DataResult.Success -> if (isExport) {
                Exported(
                    result.value.books
                )
            } else {
                Imported(result.value.books, result.value.unmatchedBooks)
            }
            is DataResult.Failure -> if (result.error == com.model.DataError.INVALID_FORMAT) NotABackup else Failed
        }
    }
}
