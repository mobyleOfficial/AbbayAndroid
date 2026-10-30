package com.mobyle.abbay.fakes

import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.settings.SettingsViewModel
import com.usecase.AddBookmark
import com.usecase.AddLibraryRoot
import com.usecase.AddSingleFileBook
import com.usecase.AddToQueue
import com.usecase.ClearLibrary
import com.usecase.ClearStatistics
import com.usecase.CompleteLegacyFolderMigration
import com.usecase.ExportBackup
import com.usecase.GetAppLanguage
import com.usecase.GetAppSettings
import com.usecase.GetLastBook
import com.usecase.GetOpusEncoder
import com.usecase.HideBook
import com.usecase.ImportBackup
import com.usecase.ImportLegacyProgress
import com.usecase.ObserveAppSettings
import com.usecase.ObserveBooks
import com.usecase.ObserveLibraryRoots
import com.usecase.ObserveLibriVoxDownloads
import com.usecase.ObserveScanStatus
import com.usecase.OpenExternalFile
import com.usecase.PrepareLegacyFolderMigration
import com.usecase.RefreshLibrary
import com.usecase.RefreshLibraryOnStart
import com.usecase.RemoveLibraryRoot
import com.usecase.RepairLibriVoxBook
import com.usecase.ReplaceLibraryRoots
import com.usecase.RequestFullScan
import com.usecase.SaveListeningState
import com.usecase.SetAppLanguage
import com.usecase.SetBookState
import com.usecase.SortAndFilterBooks
import com.usecase.UpdateAppSettings
import com.usecase.UpdateBookmarkNote

class ViewModelFactory(
    val libraryRepository: FakeLibraryRepository = FakeLibraryRepository(),
    val settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    val legacyRepository: FakeLegacyMigrationRepository = FakeLegacyMigrationRepository(),
    val playerController: FakePlayerController = FakePlayerController(),
    val listeningRepository: FakeListeningRepository = FakeListeningRepository(),
    val backupRepository: FakeBackupRepository = FakeBackupRepository(),
    val queueRepository: FakeQueueRepository = FakeQueueRepository(),
    val statisticsRepository: FakeStatisticsRepository = FakeStatisticsRepository(),
    val converterRepository: FakeConverterRepository = FakeConverterRepository(),
    val languageRepository: FakeLanguageRepository = FakeLanguageRepository(),
    val libriVoxRepository: FakeLibriVoxRepository = FakeLibriVoxRepository(),
) {
    fun booksListViewModel() = BooksListViewModel(
        playerController = playerController,
        saveListeningState = SaveListeningState(libraryRepository),
        updateAppSettings = UpdateAppSettings(settingsRepository),
        addLibraryRoot = AddLibraryRoot(libraryRepository),
        addSingleFileBook = AddSingleFileBook(libraryRepository),
        refreshLibrary = RefreshLibrary(libraryRepository),
        refreshLibraryOnStart = RefreshLibraryOnStart(libraryRepository, settingsRepository),
        requestFullScan = RequestFullScan(libraryRepository),
        hideBook = HideBook(libraryRepository),
        setBookState = SetBookState(libraryRepository),
        sortAndFilterBooks = SortAndFilterBooks(),
        prepareLegacyFolderMigration = PrepareLegacyFolderMigration(legacyRepository, libraryRepository),
        completeLegacyFolderMigration = CompleteLegacyFolderMigration(legacyRepository),
        importLegacyProgress = ImportLegacyProgress(legacyRepository),
        getAppSettings = GetAppSettings(settingsRepository),
        addBookmark = AddBookmark(listeningRepository),
        updateBookmarkNote = UpdateBookmarkNote(listeningRepository),
        getLastBook = GetLastBook(settingsRepository, libraryRepository),
        openExternalFile = OpenExternalFile(libraryRepository),
        observeBooks = ObserveBooks(libraryRepository),
        observeLibraryRoots = ObserveLibraryRoots(libraryRepository),
        observeScanStatus = ObserveScanStatus(libraryRepository),
        observeAppSettings = ObserveAppSettings(settingsRepository),
        addToQueue = AddToQueue(queueRepository),
        getOpusEncoder = GetOpusEncoder(converterRepository),
        observeLibriVoxDownloads = ObserveLibriVoxDownloads(libriVoxRepository),
        repairLibriVoxBook = RepairLibriVoxBook(libriVoxRepository),
    )

    fun settingsViewModel() = SettingsViewModel(
        updateAppSettings = UpdateAppSettings(settingsRepository),
        replaceLibraryRoots = ReplaceLibraryRoots(libraryRepository),
        addLibraryRoot = AddLibraryRoot(libraryRepository),
        removeLibraryRoot = RemoveLibraryRoot(libraryRepository),
        clearLibrary = ClearLibrary(libraryRepository),
        exportBackup = ExportBackup(backupRepository),
        importBackup = ImportBackup(backupRepository),
        clearStatistics = ClearStatistics(statisticsRepository),
        observeAppSettings = ObserveAppSettings(settingsRepository),
        observeLibraryRoots = ObserveLibraryRoots(libraryRepository),
        getAppLanguage = GetAppLanguage(languageRepository),
        setAppLanguage = SetAppLanguage(languageRepository),
    )
}
